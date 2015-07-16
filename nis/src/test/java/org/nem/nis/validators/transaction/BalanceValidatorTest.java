package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.function.Function;

@RunWith(Enclosed.class)
public class BalanceValidatorTest {

	//region fee only

	public static class FeeOnlyTransactionBalanceValidatorTest extends AbstractBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final int balanceDelta, final Function<Amount, Account> createAccount) {
			final Account sender = createAccount.apply(Amount.fromNem(37 + balanceDelta));
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.setFee(Amount.fromNem(37));
			return transaction;
		}
	}

	//endregion

	//region fee and single transfer

	public static class FeeAndSingleTransferTransactionBalanceValidatorTest extends AbstractBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final int balanceDelta, final Function<Amount, Account> createAccount) {
			final Account sender = createAccount.apply(Amount.fromNem(37 + balanceDelta));
			final Account recipient = createAccount.apply(Amount.ZERO);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.setTransferAction(observer -> {
				observer.notifyDebit(sender, transaction.getFee());
				observer.notifyTransfer(sender, recipient, Amount.fromNem(25));
			});
			transaction.setFee(Amount.fromNem(12));
			return transaction;
		}
	}

	public static class FeeAndSingleSelfTransferTransactionBalanceValidatorTest extends AbstractBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final int balanceDelta, final Function<Amount, Account> createAccount) {
			final Account sender = createAccount.apply(Amount.fromNem(37 + balanceDelta));
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.setTransferAction(observer -> {
				observer.notifyDebit(sender, transaction.getFee());
				observer.notifyTransfer(sender, sender, Amount.fromNem(25));
			});
			transaction.setFee(Amount.fromNem(12));
			return transaction;
		}
	}

	//endregion

	//region fee and multiple transfers

	public static class FeeAndMultipleTransfersTransactionBalanceValidatorTest extends AbstractBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final int balanceDelta, final Function<Amount, Account> createAccount) {
			final Account sender = createAccount.apply(Amount.fromNem(37 + balanceDelta));
			final Account recipient = createAccount.apply(Amount.ZERO);
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.setTransferAction(observer -> {
				observer.notifyDebit(sender, transaction.getFee());
				observer.notifyTransfer(sender, recipient, Amount.fromNem(11));
				observer.notifyTransfer(sender, recipient, Amount.fromNem(10));
				observer.notifyTransfer(sender, recipient, Amount.fromNem(4));
			});
			transaction.setFee(Amount.fromNem(12));
			return transaction;
		}

		@Test
		public void earlierCreditsCanBeSpentLater() {
			// Act:
			runTest(
					createAccount -> {
						final Account sender = createAccount.apply(Amount.fromNem(36));
						final Account recipient = createAccount.apply(Amount.ZERO);
						final MockTransaction transaction = new MockTransaction(sender);
						transaction.setTransferAction(observer -> {
							observer.notifyDebit(sender, transaction.getFee());
							observer.notifyTransfer(sender, recipient, Amount.fromNem(11));
							observer.notifyTransfer(sender, recipient, Amount.fromNem(10));
							observer.notifyTransfer(recipient, sender, Amount.fromNem(10));
							observer.notifyTransfer(sender, recipient, Amount.fromNem(4));
						});
						transaction.setFee(Amount.fromNem(12));
						return transaction;
					},
					ValidationResult.SUCCESS);
		}

		@Test
		public void cannotPreSpentLaterCreditsEarlier() {
			// Act:
			runTest(
					createAccount -> {
						final Account sender = createAccount.apply(Amount.fromNem(36));
						final Account recipient = createAccount.apply(Amount.ZERO);
						final MockTransaction transaction = new MockTransaction(sender);
						transaction.setTransferAction(observer -> {
							observer.notifyDebit(sender, transaction.getFee());
							observer.notifyTransfer(sender, recipient, Amount.fromNem(11));
							observer.notifyTransfer(sender, recipient, Amount.fromNem(10));
							observer.notifyTransfer(sender, recipient, Amount.fromNem(4));
							observer.notifyTransfer(recipient, sender, Amount.fromNem(10));
						});
						transaction.setFee(Amount.fromNem(12));
						return transaction;
					},
					ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		}
	}

	//endregion

	//region fee and multiple transfers from multiple senders

	public static class FeeAndMultipleTransfersFromMultipleSendersTransactionBalanceValidatorTest extends AbstractBalanceValidatorTest {

		@Override
		protected Transaction createTransaction(final int balanceDelta, final Function<Amount, Account> createAccount) {
			final Account sender1 = createAccount.apply(Amount.fromNem(10));
			final Account sender2 = createAccount.apply(Amount.fromNem(37 + balanceDelta));
			final Account sender3 = createAccount.apply(Amount.fromNem(10));
			final Account recipient = createAccount.apply(Amount.ZERO);
			final MockTransaction transaction = new MockTransaction(sender2);
			transaction.setTransferAction(observer -> {
				observer.notifyDebit(sender2, transaction.getFee());
				observer.notifyTransfer(sender1, recipient, Amount.fromNem(10));
				observer.notifyTransfer(sender2, recipient, Amount.fromNem(25));
				observer.notifyTransfer(sender3, recipient, Amount.fromNem(10));
			});
			transaction.setFee(Amount.fromNem(12));
			return transaction;
		}
	}

	//endregion

	private static abstract class AbstractBalanceValidatorTest {

		@Test
		public void accountWithLargerThanRequiredBalancePassesValidation() {
			// Assert:
			this.assertValidationResult(1, ValidationResult.SUCCESS);
		}

		@Test
		public void accountWithExactRequiredBalancePassesValidation() {
			// Assert:
			this.assertValidationResult(0, ValidationResult.SUCCESS);
		}

		@Test
		public void accountWithSmallerThanRequiredBalanceFailsValidation() {
			// Assert:
			this.assertValidationResult(-1, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		}

		protected abstract Transaction createTransaction(
				final int balanceDelta,
				Function<Amount, Account> createAccount);

		protected static void runTest(
				final Function<Function<Amount, Account>, Transaction> createTransaction,
				final ValidationResult expectedResult) {
			// Arrange:
			final Map<Account, Amount> accountBalanceMap = new HashMap<>();
			final DebitPredicate debitPredicate = (account, amount) ->
					accountBalanceMap.getOrDefault(account, Amount.ZERO).compareTo(amount) >= 0;

			final Function<Amount, Account> createAccount = balance -> {
				final Account account = Utils.generateRandomAccount();
				accountBalanceMap.put(account, balance);
				return account;
			};

			final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
			final SingleTransactionValidator validator = new BalanceValidator(accountStateCache);
			final Transaction transaction = createTransaction.apply(createAccount);

			// Act:
			final ValidationResult result = validator.validate(transaction, new ValidationContext(debitPredicate));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}

		private void assertValidationResult(final int balanceDelta, final ValidationResult expectedResult) {
			// Act:
			runTest(
					createAccount -> this.createTransaction(balanceDelta, createAccount),
					expectedResult);
		}
	}

	public static class SmartTileBalanceValidatorTest {
		private final Account sender = Utils.generateRandomAccount();
		private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		private final AccountState accountState = Mockito.mock(AccountState.class);
		private final SmartTileMap smartTileMap = new SmartTileMap();
		private final SingleTransactionValidator validator = new BalanceValidator(this.accountStateCache);

		public SmartTileBalanceValidatorTest() {
			Mockito.when(this.accountStateCache.findStateByAddress(this.sender.getAddress())).thenReturn(this.accountState);
			Mockito.when(this.accountState.getSmartTileMap()).thenReturn(this.smartTileMap);
			this.smartTileMap.add(createSmartTile(123));
		}

		public static SmartTile createSmartTile(final long quantity) {
			return new SmartTile(Utils.createMosaicId(1), Quantity.fromValue(quantity));
		}

		protected Transaction createTransaction(final Account sender, final SmartTile smartTile) {
			final Account recipient = Utils.generateRandomAccount();
			final MockTransaction transaction = new MockTransaction(sender);
			transaction.setTransferAction(observer -> {
				observer.notifyTransfer(sender, recipient, smartTile);
			});
			transaction.setFee(Amount.ZERO);
			return transaction;
		}

		@Test
		public void accountWithLargerThanRequiredSmartTileBalancePassesValidation() {
			assertValidationResult(122, ValidationResult.SUCCESS);
		}

		@Test
		public void accountWithExactRequiredSmartTileBalancePassesValidation() {
			assertValidationResult(123, ValidationResult.SUCCESS);
		}

		@Test
		public void accountWithSmallerThanRequiredSmartTileBalanceFailsValidation() {
			assertValidationResult(124, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		}

		private void assertValidationResult(final long quantity, final ValidationResult expectedResult) {
			// Arrange:
			final Transaction transaction = this.createTransaction(this.sender, createSmartTile(quantity));

			// Act:
			final ValidationResult result = validator.validate(transaction, new ValidationContext(null));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}
	}
}
