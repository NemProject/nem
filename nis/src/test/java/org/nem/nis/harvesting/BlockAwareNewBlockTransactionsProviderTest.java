package org.nem.nis.harvesting;

import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.*;

import java.util.*;

public class BlockAwareNewBlockTransactionsProviderTest {

	@Test
	public void v1ProviderIsUsedBeforeForkHeight() {
		// Assert:
		assertV1Behavior(new BlockHeight(BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK - 1));
	}

	@Test
	public void latestProviderIsUsedAtBlockHeight() {
		assertLatestBehavior(new BlockHeight(BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK));
	}

	@Test
	public void latestProviderIsUsedAfterBlockHeight() {
		assertLatestBehavior(new BlockHeight(BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK + 1));
	}

	private static void assertV1Behavior(final BlockHeight height) {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(
				Utils.generateRandomAddress(),
				new TimeInstant(10));

		// Assert:
		Assert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Arrays.asList(transactions.get(0))));
	}

	private static void assertLatestBehavior(final BlockHeight height) {
		// Arrange:
		final TestContext context = new TestContext(new TransferTransactionValidator());
		final Account account1 = context.addAccount(Amount.fromNem(5));
		final Account account2 = context.addAccount(Amount.fromNem(100));
		final List<Transaction> transactions = Arrays.asList(
				new TransferTransaction(new TimeInstant(1), account2, account1, Amount.fromNem(10), null),
				new TransferTransaction(new TimeInstant(2), account1, account2, Amount.fromNem(6), null));
		transactions.forEach(t -> t.setDeadline(new TimeInstant(3600)));
		context.addTransactions(transactions);

		// Act:
		final List<Transaction> filteredTransactions = context.provider.getBlockTransactions(
				Utils.generateRandomAddress(),
				new TimeInstant(10));

		// Assert:
		Assert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(transactions));
	}

	public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
		final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
		transferTransaction.setDeadline(timeStamp.addSeconds(1));
		return transferTransaction;
	}

	private static class TestContext {
		protected final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final TransactionValidatorFactory validatorFactory;
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final List<Transaction> transactions = new ArrayList<>();

		protected final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
		protected final NewBlockTransactionsProvider provider;

		private TestContext(final TransferTransactionValidator singleValidator) {
			this(new TSingleTransactionValidatorAdapter<>(TransactionTypes.TRANSFER, singleValidator));
		}

		private TestContext(final SingleTransactionValidator singleValidator) {
			this(createMockValidatorFactory(singleValidator));
		}

		private static TransactionValidatorFactory createMockValidatorFactory(final SingleTransactionValidator singleValidator) {
			final TransactionValidatorFactory validatorFactory = Mockito.mock(TransactionValidatorFactory.class);
			Mockito.when(validatorFactory.createSingle(Mockito.any())).thenReturn(singleValidator);
			return validatorFactory;
		}

		private TestContext(final TransactionValidatorFactory validatorFactory) {
			this.validatorFactory = validatorFactory;
			Mockito.when(this.unconfirmedTransactions.getTransactionsBefore(Mockito.any())).thenReturn(this.transactions);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);

			this.provider = new NewBlockTransactionsProviderV1(
					this.nisCache,
					this.validatorFactory,
					this.unconfirmedTransactions);
		}

		//region addAccount

		public Account addAccount(final Amount amount) {
			return this.prepareAccount(Utils.generateRandomAccount(), amount);
		}

		public Account prepareAccount(final Account account, final Amount amount) {
			final AccountState accountState = new AccountState(account.getAddress());
			accountState.getAccountInfo().incrementBalance(amount);
			Mockito.when(this.accountStateCache.findStateByAddress(account.getAddress())).thenReturn(accountState);
			return account;
		}

		//endregion

		//region addTransaction

		public void addTransaction(final Transaction transaction) {
			this.transactions.add(transaction);
		}

		public void addTransactions(final Collection<? extends Transaction> transactions) {
			this.transactions.addAll(transactions);
		}

		public void addTransactions(final Account signer, final int startCustomField, final int endCustomField) {
			for (int i = startCustomField; i <= endCustomField; ++i) {
				this.addTransaction(new MockTransaction(signer, i));
			}
		}

		//endregion
	}
}