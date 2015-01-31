package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.*;

public class TransactionSpamFilterTest {

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
	@Test
	public void transactionIsNotPermissibleIfCacheAtLeastMaxAllowedTransactionPerBlockAndDebtorHasZeroImportance() {
		// Arrange:
		final TestContext context = new TestContext(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(1);

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
		final Collection<Transaction> transactions = createTransactions(100);

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions.size(), IsEqual.equalTo(5));
	}

	// TODO 20150130 J-B: consider a few variations of this test, i.e.
	// > assertNumPermissibleTransactions(0.01, ???);
	// > assertNumPermissibleTransactions(0.001, 838);
	// > assertNumPermissibleTransactions(0.0001, ???);
	@Test
	public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToFairShareOfDebtor() {
		// Arrange:
		final TestContext context = new TestContext(0, BlockHeight.ONE);

		// 0.001 * e^(-y/300) * 1000 * (1000 - y) / 10 = 1 has solution y = 837.123...
		context.setImportance(0.001);
		final Collection<Transaction> transactions = createTransactions(1000);

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions.size(), IsEqual.equalTo(838));
	}

	// TODO 20150130 J-B: consider four variations of this test, i.e.
	// > [high|low] fee + [high|low] importance
	@Test
	public void highFeeHelpsToPlaceTransactionIntoCache() {
		// Arrange:
		final TestContext context = new TestContext(800, BlockHeight.ONE);
		context.setImportance(0.0);
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(Arrays.asList(transaction));

		// Assert:
		Assert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(Arrays.asList(transaction)));
	}

	// endregion

	// region delegation

	@Test
	public void filterDelegatesToUnderlyingCaches() {
		// Arrange:
		final TestContext context = new TestContext(200, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(1);

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

	private static Collection<Transaction> createTransactions(final int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> {
					final Transaction transaction = new MockTransaction(Utils.generateRandomAccount());
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
