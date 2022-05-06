package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisTestConstants;

import java.util.*;
import java.util.stream.*;

@RunWith(Enclosed.class)
public class TransactionSpamFilterTest {
	private static final boolean USE_SINGLE_ACCOUNT = true;
	private static final boolean USE_DIFFERENT_ACCOUNTS = false;
	private static final int MAX_TRANSACTIONS_PER_BLOCK = NisTestConstants.MAX_TRANSACTIONS_PER_BLOCK;

	// region filter

	private abstract static class TransactionSpamFilterTestBase {
		@Test
		public void anyTransactionIsPermissibleIfCacheHasLessTransactionsThanMaxAllowedTransactionPerBlock() {
			// Arrange:
			final TestContext context = new TestContext(MAX_TRANSACTIONS_PER_BLOCK - 1, BlockHeight.ONE, this.useMultisig());
			context.setImportance(0.0);
			final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
			transaction.setFee(Amount.fromNem(0));

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(Collections.singletonList(transaction));

			// Assert:
			MatcherAssert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Collections.singletonList(transaction)));
		}

		@Test
		public void transactionWithZeroFeeIsNotPermissibleIfCacheSizeIsAtLeastMaxAllowedTransactionsPerBlockAndDebtorHasZeroImportance() {
			// Arrange:
			final TestContext context = new TestContext(MAX_TRANSACTIONS_PER_BLOCK, BlockHeight.ONE, this.useMultisig());
			context.setImportance(0.0);
			final Collection<Transaction> transactions = this.createTransactions(USE_SINGLE_ACCOUNT, 1); // this transaction has zero fee

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

			// Assert:
			MatcherAssert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Collections.emptyList()));
		}

		@Test
		public void transactionWithHighFeeIsPermissibleIfCacheSizeIsAtLeastMaxAllowedTransactionsPerBlockAndDebtorHasImportanceNotSet() {
			// Arrange:
			final TestContext context = new TestContext(MAX_TRANSACTIONS_PER_BLOCK, BlockHeight.ONE, this.useMultisig());
			context.setImportanceHeight(new BlockHeight(2));
			final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
			transaction.setFee(Amount.fromNem(100));

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(Collections.singletonList(transaction));

			// Assert:
			MatcherAssert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Collections.singletonList(transaction)));
		}

		@Test
		public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToMaxAllowedTransactionPerBlockIfAllDebtorsHaveZeroImportance() {
			// Arrange:
			final TestContext context = new TestContext(MAX_TRANSACTIONS_PER_BLOCK - 5, BlockHeight.ONE, this.useMultisig());
			context.setImportance(0.0);
			final Collection<Transaction> transactions = this.createTransactions(USE_DIFFERENT_ACCOUNTS, 100); // all have zero fee

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

			// Assert:
			MatcherAssert.assertThat(filteredTransactions.size(), IsEqual.equalTo(5));
		}

		@Test
		public void filterNeverAllowsMoreThanMaxCacheSizeTransactions() {
			// Arrange:
			final Collection<Transaction> transactions = this.createTransactions(USE_DIFFERENT_ACCOUNTS, 2100);

			// Assert:
			this.assertFilteredTransactionsSize(transactions, 1, 1200);
		}

		@Test
		public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToFairShareOfDebtorForDifferentAccounts() {
			// Arrange:
			// - different accounts fill the cache
			// - rounded solutions for equation: importance * e^(-3 * y / 1200) * 100 * (1200 - y) = 1
			final double[] importanceArray = {
					1, 0.1, 0.01, 0.001, 0.0001, 0.00001
			};
			final int[] expectedCacheSizeDifferentAccounts = {
					1200, 1199, 1181, 1059, 669, 120
			};
			final Collection<Transaction> transactions = this.createTransactions(USE_DIFFERENT_ACCOUNTS, 2100);

			for (int i = 0; i < importanceArray.length; i++) {
				// Assert:
				this.assertFilteredTransactionsSize(transactions, importanceArray[i], expectedCacheSizeDifferentAccounts[i]);
			}
		}

		@Test
		public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToFairShareOfDebtorForSingleAccount() {
			// Arrange:
			// - single account fills the cache
			// - rounded solutions for equation: importance * e^(-3 * y / 2000) * 100 * (2000 - y) = y
			// - for importance <= 0.001, only max transactions per block (200) are allowed when cache is initially empty
			final double[] importanceArray = {
					1, 0.1, 0.01, 0.001, 0.0001, 0.00001
			};
			final int[] expectedCacheSizeSingleAccount = {
					1054, 736, 352, 120, 120, 120
			};
			final Collection<Transaction> transactions = this.createTransactions(USE_SINGLE_ACCOUNT, 2100);

			for (int i = 0; i < importanceArray.length; i++) {
				// Assert:
				this.assertFilteredTransactionsSize(transactions, importanceArray[i], expectedCacheSizeSingleAccount[i]);
			}
		}

		@Test
		public void filterResultDependsOnImportanceAndFeeWhenCacheHasHighFillLevel() {
			// Assert: boolean parameter says whether transaction is filtered or not.
			final int cacheSize = 1000;
			this.assertFilterResult(cacheSize, 0.0, 0, true); // no importance and fee
			this.assertFilterResult(cacheSize, 0.0, 100000, true); // no importance, fee not high enough
			this.assertFilterResult(cacheSize, 0.0, 1000000, false); // no importance, high fee

			this.assertFilterResult(cacheSize, 0.0001, 0, true); // medium importance, no fee
			this.assertFilterResult(cacheSize, 0.0001, 100000, true); // medium importance, medium fee

			this.assertFilterResult(cacheSize, 0.01, 0, false); // high importance and no fee
			this.assertFilterResult(cacheSize, 0.01, 1000000, false); // high importance and fee
		}

		@Test
		public void filterResultDependsOnImportanceAndFeeWhenCacheHasHighMediumLevel() {
			// Assert: boolean parameter says whether transaction is filtered or not.
			final int cacheSize = 250;
			this.assertFilterResult(cacheSize, 0.0, 0, true); // no importance and fee
			this.assertFilterResult(cacheSize, 0.0, 100000, false); // no importance, fee not high enough
			this.assertFilterResult(cacheSize, 0.0, 1000000, false); // no importance, high fee

			this.assertFilterResult(cacheSize, 0.0001, 0, false); // medium importance, no fee
			this.assertFilterResult(cacheSize, 0.0001, 100000, false); // medium importance, medium fee

			this.assertFilterResult(cacheSize, 0.01, 0, false); // high importance and no fee
			this.assertFilterResult(cacheSize, 0.01, 1000000, false); // high importance and fee
		}

		@Test
		public void filterResultDependsOnImportanceAndFeeWhenCacheHasLowFillLevel() {
			// Assert: boolean parameter says whether transaction is filtered or not.
			final int cacheSize = 100;
			this.assertFilterResult(cacheSize, 0.0, 0, false); // no importance and fee
			this.assertFilterResult(cacheSize, 0.0, 100000, false); // no importance, fee not high enough
			this.assertFilterResult(cacheSize, 0.0, 1000000, false); // no importance, high fee

			this.assertFilterResult(cacheSize, 0.0001, 0, false); // medium importance, no fee
			this.assertFilterResult(cacheSize, 0.0001, 100000, false); // medium importance, medium fee

			this.assertFilterResult(cacheSize, 0.01, 0, false); // high importance and no fee
			this.assertFilterResult(cacheSize, 0.01, 1000000, false); // high importance and fee
		}

		private void assertFilteredTransactionsSize(final Collection<Transaction> transactions, final double importance,
				final int expectedFilteredTransactionsSize) {
			// Arrange:
			final TestContext context = new TestContext(0, BlockHeight.ONE, this.useMultisig());
			context.setImportance(importance);

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

			// Assert:
			MatcherAssert.assertThat(filteredTransactions.size(), IsEqual.equalTo(expectedFilteredTransactionsSize));
		}

		private void assertFilterResult(final int currentCacheSize, final double importance, final long fee, final boolean isFiltered) {
			// Arrange:
			final TestContext context = new TestContext(currentCacheSize, BlockHeight.ONE, this.useMultisig());
			context.setImportance(importance);
			final Transaction transaction = this.useMultisig()
					? new MockTransaction(TransactionTypes.MULTISIG, Utils.generateRandomAccount())
					: new MockTransaction(Utils.generateRandomAccount());
			transaction.setFee(Amount.fromMicroNem(fee));

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(Collections.singletonList(transaction));

			// Assert:
			MatcherAssert.assertThat(filteredTransactions.isEmpty(), IsEqual.equalTo(isFiltered));
		}

		protected abstract Collection<Transaction> createTransactions(final boolean useSingleAccount, final int count);

		protected abstract boolean useMultisig();

		// endregion
	}

	public static class NonMultisigTest extends TransactionSpamFilterTestBase {
		protected Collection<Transaction> createTransactions(final boolean useSingleAccount, final int count) {
			return createNonMultisigTransactions(useSingleAccount, count);
		}

		protected boolean useMultisig() {
			return false;
		}
	}

	public static class MultisigTest extends TransactionSpamFilterTestBase {
		protected Collection<Transaction> createTransactions(final boolean useSingleAccount, final int count) {
			return createMultisigTransactions(useSingleAccount, count);
		}

		protected boolean useMultisig() {
			return true;
		}
	}

	public static class AdditionalSpamFilterTests {
		// region mix multisig / non-multisig

		@Test
		public void canAddMultisigTransactionsWhenNonMultisigPartIsFull() {
			// Arrange:
			final TestContext context = new TestContext(1200, BlockHeight.ONE, false);
			final Collection<Transaction> transactions = createMultisigTransactions(USE_SINGLE_ACCOUNT, 120);

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

			// Assert:
			MatcherAssert.assertThat(filteredTransactions.size(), IsEqual.equalTo(120));
			MatcherAssert.assertThat(filteredTransactions, IsEqual.equalTo(transactions));
		}

		@Test
		public void canAddNonMultisigTransactionsWhenMultisigPartIsFull() {
			// Arrange:
			final TestContext context = new TestContext(1200, BlockHeight.ONE, true);
			final Collection<Transaction> transactions = createNonMultisigTransactions(USE_SINGLE_ACCOUNT, 120);

			// Act:
			final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

			// Assert:
			MatcherAssert.assertThat(filteredTransactions.size(), IsEqual.equalTo(120));
			MatcherAssert.assertThat(filteredTransactions, IsEqual.equalTo(transactions));
		}

		// endregion

		// region delegation

		@Test
		public void filterDelegatesToUnderlyingCaches() {
			// Arrange:
			final TestContext context = new TestContext(200, BlockHeight.ONE, false);
			context.setImportance(0.0);
			final Collection<Transaction> transactions = createNonMultisigTransactions(USE_SINGLE_ACCOUNT, 1);

			// Act:
			context.spamFilter.filter(transactions);

			// Assert:
			Mockito.verify(context.nisCache, Mockito.times(1)).getAccountStateCache();
			Mockito.verify(context.nisCache, Mockito.times(1)).getPoxFacade();
			Mockito.verify(context.transactions, Mockito.times(1)).contains(Mockito.any());
			Mockito.verify(context.transactions, Mockito.times(1)).stream();
		}

		// endregion
	}

	private static Collection<Transaction> createNonMultisigTransactions(final boolean useSingleAccount, final int count) {
		final Account account = Utils.generateRandomAccount();
		return IntStream.range(0, count).mapToObj(i -> {
			final Transaction transaction = new MockTransaction(useSingleAccount ? account : Utils.generateRandomAccount());
			transaction.setFee(Amount.ZERO);
			return transaction;
		}).collect(Collectors.toList());
	}

	private static Collection<Transaction> createMultisigTransactions(final boolean useSingleAccount, final int count) {
		final Account account = Utils.generateRandomAccount();
		return IntStream.range(0, count).mapToObj(i -> {
			final Transaction transaction = new MockTransaction(TransactionTypes.MULTISIG,
					useSingleAccount ? account : Utils.generateRandomAccount());
			transaction.setFee(Amount.ZERO);
			return transaction;
		}).collect(Collectors.toList());
	}

	private static class TestContext {
		private final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final UnconfirmedTransactionsCache transactions = Mockito.spy(new UnconfirmedTransactionsCache());
		private final TransactionSpamFilter spamFilter = new TransactionSpamFilter(this.nisCache, this.transactions,
				MAX_TRANSACTIONS_PER_BLOCK);
		private final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);
		private final boolean useMultisig;

		private TestContext(final int transactionsSize, final BlockHeight lastRecalculationHeight, final boolean useMultisig) {
			final PoxFacade poxFacade = Mockito.mock(PoxFacade.class);
			final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
			final AccountState state = Mockito.mock(AccountState.class);
			Mockito.when(this.nisCache.getPoxFacade()).thenReturn(poxFacade);
			Mockito.when(poxFacade.getLastRecalculationHeight()).thenReturn(lastRecalculationHeight);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(accountStateCache);
			Mockito.when(accountStateCache.findStateByAddress(Mockito.any())).thenReturn(state);
			Mockito.when(state.getImportanceInfo()).thenReturn(this.accountImportance);
			Mockito.when(this.accountImportance.getHeight()).thenReturn(BlockHeight.ONE);
			this.useMultisig = useMultisig;
			this.fillCache(transactionsSize);
		}

		private void setImportance(final double importance) {
			Mockito.when(this.accountImportance.getImportance(BlockHeight.ONE)).thenReturn(importance);
		}

		private void setImportanceHeight(final BlockHeight height) {
			Mockito.when(this.accountImportance.getHeight()).thenReturn(height);
		}

		private void fillCache(final int count) {
			for (int i = 0; i < count; i++) {
				this.transactions.add(this.useMultisig
						? new MockTransaction(TransactionTypes.MULTISIG, Utils.generateRandomAccount())
						: new MockTransaction(Utils.generateRandomAccount()));
			}
		}
	}
}
