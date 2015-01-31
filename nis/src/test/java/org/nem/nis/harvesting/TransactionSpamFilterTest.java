package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.*;

public class TransactionSpamFilterTest {
	// importances for the tests
	private static final double[] importanceArray = { 1, 0.1, 0.01, 0.001, 0.0001, 0.00001 };

	// Different accounts fills the cache: rounded solutions for equation importance * e^(-y/300) * 1000 * (1000 - y) / 10 = 1
	private static final int[] expectedCacheSizeDifferentAccounts = { 1000, 998, 975, 838, 490, 120 };

	// Single account fills the cache: rounded solutions for equation importance * e^(-y/300) * 1000 * (1000 - y) / 10 = y
	private static final int[] expectedCacheSizeSingleAccount = { 854, 587, 281, 120, 120, 120 };

	private static boolean USE_SINGLE_ACCOUNT = true;

	// region filter

	@Test
	public void anyTransactionIsPermissibleIfCacheHasLessTransactionsThanMaxAllowedTransactionPerBlock() {
		// Arrange:
		final TestContext context = new TestContext(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1, BlockHeight.ONE);
		context.setImportance(0.0);
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setFee(Amount.fromNem(0));

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(Arrays.asList(transaction));

		// Assert:
		Assert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Arrays.asList(transaction)));
	}

	// TODO 20150130 J-B: i think the name is misleading as "permission" is a function of both importance and fee (below a zero importance transaction is
	// > allowed because it has a high fee)
	// TODO 20150131 BR -> J: changed name
	@Test
	public void transactionWithZeroFeeIsNotPermissibleIfCacheSizeIsAtLeastMaxAllowedTransactionsPerBlockAndDebtorHasZeroImportance() {
		// Arrange:
		final TestContext context = new TestContext(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(USE_SINGLE_ACCOUNT, 1); // this transaction has zero fee

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Arrays.asList()));
	}

	@Test
	public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToMaxAllowedTransactionPerBlockIfAllDebtorsHaveZeroImportance() {
		// Arrange:
		final TestContext context = new TestContext(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 5, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(!USE_SINGLE_ACCOUNT, 100); // all transactions have zero fee

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions.size(), IsEqual.equalTo(5));
	}

	// TODO 20150130 J-B: consider a few variations of this test, i.e.
	// > assertNumPermissibleTransactions(0.01, ???);
	// > assertNumPermissibleTransactions(0.001, 838);
	// > assertNumPermissibleTransactions(0.0001, ???);
	// TODO 20150131 BR -> J: ok
	@Test
	public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToFairShareOfDebtorForDifferentAccounts() {
		final Collection<Transaction> transactions = createTransactions(!USE_SINGLE_ACCOUNT, 1000);

		for (int i = 0; i < importanceArray.length; i++) {
			// Assert:
			this.assertFilteredTransactionsSize(transactions, importanceArray[i], expectedCacheSizeDifferentAccounts[i]);
		}
	}

	@Test
	public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToFairShareOfDebtorForSingleAccount() {
		final Collection<Transaction> transactions = createTransactions(USE_SINGLE_ACCOUNT, 1000);

		for (int i = 0; i < importanceArray.length; i++) {
			// Assert:
			this.assertFilteredTransactionsSize(transactions, importanceArray[i], expectedCacheSizeSingleAccount[i]);
		}
	}

	// TODO 20150130 J-B: consider four variations of this test, i.e.
	// > [high|low] fee + [high|low] importance
	// TODO 20150131 BR -> J: ok (btw. the approach is not exact science, the filter function used is a proposal)
	@Test
	public void filterResultDependsOnImportanceAndFeeAndCurrentCacheSize() {
		// Assert: boolean parameter says whether transaction is filtered or not.
		// cache with high fill level
		this.assertFilterResult(800, 0.0, 100, false);		// no importance, high fee
		this.assertFilterResult(800, 0.0, 10, true);		// no importance, fee not high enough
		this.assertFilterResult(800, 0.01, 0, false);		// high importance, no fee
		this.assertFilterResult(800, 0.0001, 0, true);		// importance not high enough, no fee
		this.assertFilterResult(800, 0.01, 100, false);		// high importance and fee
		this.assertFilterResult(800, 0.0, 0, true);			// no importance and fee

		// cache with medium fill level
		this.assertFilterResult(250, 0.0, 10, false);		// no importance, medium fee
		this.assertFilterResult(250, 0.0001, 0, false);		// medium importance, no fee
		this.assertFilterResult(250, 0.0001, 10, false);	// medium importance, medium fee
		this.assertFilterResult(250, 0.0, 0, true);			// no importance and fee

		// cache with low fill level
		this.assertFilterResult(100, 0.0, 0, false);		// no importance and fee
	}

	private void assertFilteredTransactionsSize(
			final Collection<Transaction> transactions,
			final double importance,
			final int expectedFilteredTransactionsSize) {
		// Arrange:
		final TestContext context = new TestContext(0, BlockHeight.ONE);

		context.setImportance(importance);

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions.size(), IsEqual.equalTo(expectedFilteredTransactionsSize));
	}

	private void assertFilterResult(
			final int currentCacheSize,
			final double importance,
			final long fee,
			final boolean isFiltered) {
		// Arrange:
		final TestContext context = new TestContext(currentCacheSize, BlockHeight.ONE);
		context.setImportance(importance);
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setFee(Amount.fromNem(fee));

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(Arrays.asList(transaction));

		// Assert:
		Assert.assertThat(filteredTransactions.isEmpty(), IsEqual.equalTo(isFiltered));
	}

	// endregion

	// region delegation

	@Test
	public void filterDelegatesToUnderlyingCaches() {
		// Arrange:
		final TestContext context = new TestContext(200, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(USE_SINGLE_ACCOUNT, 1);

		// Act:
		context.spamFilter.filter(transactions);

		// Assert:
		Mockito.verify(context.nisCache, Mockito.times(1)).getAccountStateCache();
		Mockito.verify(context.nisCache, Mockito.times(1)).getPoiFacade();
		Mockito.verify(context.transactions, Mockito.times(1)).contains(Mockito.any());
		Mockito.verify(context.transactions, Mockito.times(2)).flatSize();
		Mockito.verify(context.transactions, Mockito.times(1)).stream();
	}

	// endregion

	private static Collection<Transaction> createTransactions(final boolean useSingleAccount, final int count) {
		final Account account = Utils.generateRandomAccount();
		return IntStream.range(0, count)
				.mapToObj(i -> {
					final Transaction transaction = new MockTransaction(useSingleAccount ? account : Utils.generateRandomAccount());
					transaction.setFee(Amount.ZERO);
					return transaction;
				})
				.collect(Collectors.toList());
	}

	private class TestContext {
		private final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final UnconfirmedTransactionsCache transactions = Mockito.spy(new UnconfirmedTransactionsCache());
		private final TransactionSpamFilter spamFilter = new TransactionSpamFilter(nisCache, transactions);
		private final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);

		private TestContext(final int transactionsSize, final BlockHeight lastPoiRecalculationHeight) {
			final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
			final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
			final AccountState state = Mockito.mock(AccountState.class);
			Mockito.when(this.nisCache.getPoiFacade()).thenReturn(poiFacade);
			Mockito.when(poiFacade.getLastPoiRecalculationHeight()).thenReturn(lastPoiRecalculationHeight);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(accountStateCache);
			Mockito.when(accountStateCache.findStateByAddress(Mockito.any())).thenReturn(state);
			Mockito.when(state.getImportanceInfo()).thenReturn(this.accountImportance);
			Mockito.when(this.accountImportance.getHeight()).thenReturn(BlockHeight.ONE);
			this.fillCache(transactionsSize);
		}

		private void setImportance(final double importance) {
			Mockito.when(this.accountImportance.getImportance(BlockHeight.ONE)).thenReturn(importance);
		}

		private void fillCache(final int count) {
			for (int i = 0; i < count; i++) {
				this.transactions.add(new MockTransaction(Utils.generateRandomAccount()));
			}
		}
	}
}
