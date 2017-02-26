package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.core.model.*;
import org.nem.core.model.observers.AccountNotification;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.function.Function;

@RunWith(Enclosed.class)
public class BalanceValidatorTest {

	public static class NoBalanceChangeBalanceValidatorTest {

		@Test
		public void otherNotificationsAreIgnored() {
			// Arrange:
			final SingleTransactionValidator validator = new BalanceValidator();
			final MockTransaction transaction = new MockTransaction();
			transaction.setTransferAction(observer -> new AccountNotification(transaction.getSigner()));

			// Act:
			final ValidationResult result = validator.validate(transaction, new ValidationContext(ValidationStates.Throw));

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		}
	}

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
				NotificationUtils.notifyDebit(observer, sender, transaction.getFee());
				NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(25));
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
				NotificationUtils.notifyDebit(observer, sender, transaction.getFee());
				NotificationUtils.notifyTransfer(observer, sender, sender, Amount.fromNem(25));
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
				NotificationUtils.notifyDebit(observer, sender, transaction.getFee());
				NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(11));
				NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(10));
				NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(4));
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
							NotificationUtils.notifyDebit(observer, sender, transaction.getFee());
							NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(11));
							NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(10));
							NotificationUtils.notifyTransfer(observer, recipient, sender, Amount.fromNem(10));
							NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(4));
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
							NotificationUtils.notifyDebit(observer, sender, transaction.getFee());
							NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(11));
							NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(10));
							NotificationUtils.notifyTransfer(observer, sender, recipient, Amount.fromNem(4));
							NotificationUtils.notifyTransfer(observer, recipient, sender, Amount.fromNem(10));
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
				NotificationUtils.notifyDebit(observer, sender2, transaction.getFee());
				NotificationUtils.notifyTransfer(observer, sender1, recipient, Amount.fromNem(10));
				NotificationUtils.notifyTransfer(observer, sender2, recipient, Amount.fromNem(25));
				NotificationUtils.notifyTransfer(observer, sender3, recipient, Amount.fromNem(10));
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
			final DebitPredicate<Amount> debitPredicate = (account, amount) ->
					accountBalanceMap.getOrDefault(account, Amount.ZERO).compareTo(amount) >= 0;

			final Function<Amount, Account> createAccount = balance -> {
				final Account account = Utils.generateRandomAccount();
				accountBalanceMap.put(account, balance);
				return account;
			};

			final SingleTransactionValidator validator = new BalanceValidator();
			final Transaction transaction = createTransaction.apply(createAccount);

			// Act:
			final ValidationState validationState = new ValidationState(debitPredicate, DebitPredicates.MosaicThrow, null);
			final ValidationResult result = validator.validate(transaction, new ValidationContext(validationState));

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
}