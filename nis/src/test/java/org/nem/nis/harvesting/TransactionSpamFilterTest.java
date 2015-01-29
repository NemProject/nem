package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.*;

public class TransactionSpamFilterTest {

	@Test
	public void anyTransactionIsPermissibleIfCacheHasLessTransactionsThanMaxAllowedTransactionPerBlock() {
		// Arrange:
		final TestContext context = new TestContext(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 1, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(1);

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions, IsEquivalent.equivalentTo(transactions));
	}

	@Test
	public void transactionIsNotPermissibleIfCacheAtLeastMaxAllowedTransactionPerBlockAndDebitorHasZeroImportance() {
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
	public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToMaxAllowedTransactionPerBlockIfAllDebitorsHaveZeroImportance() {
		// Arrange:
		final TestContext context = new TestContext(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK - 5, BlockHeight.ONE);
		context.setImportance(0.0);
		final Collection<Transaction> transactions = createTransactions(100);

		// Act:
		final Collection<Transaction> filteredTransactions = context.spamFilter.filter(transactions);

		// Assert:
		Assert.assertThat(filteredTransactions.size(), IsEqual.equalTo(5));
	}

	@Test
	public void filterReturnsExactlyEnoughTransactionsToFillTheCacheUpToFairShareOfDebitor() {
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

	private static Collection<Transaction> createTransactions(final int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> new MockTransaction(Utils.generateRandomAccount()))
				.collect(Collectors.toList());
	}

	private class TestContext {
		private final ReadOnlyNisCache nisCache = Mockito.mock(ReadOnlyNisCache.class);
		private final ConcurrentMap transactions = Mockito.mock(ConcurrentMap.class);
		private final TransactionSpamFilter spamFilter = new TransactionSpamFilter(nisCache, transactions);
		private final AccountImportance accountImportance = Mockito.mock(AccountImportance.class);

		private TestContext(final int transactionsSize, final BlockHeight lastPoiRecalculationHeight) {
			final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
			final AccountStateCache accounStateCache = Mockito.mock(AccountStateCache.class);
			final AccountState state = Mockito.mock(AccountState.class);
			Mockito.when(this.nisCache.getPoiFacade()).thenReturn(poiFacade);
			Mockito.when(poiFacade.getLastPoiRecalculationHeight()).thenReturn(lastPoiRecalculationHeight);
			Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(accounStateCache);
			Mockito.when(accounStateCache.findStateByAddress(Mockito.any())).thenReturn(state);
			Mockito.when(state.getImportanceInfo()).thenReturn(this.accountImportance);
			Mockito.when(this.accountImportance.getHeight()).thenReturn(BlockHeight.ONE);
			Mockito.when(this.transactions.size()).thenReturn(transactionsSize);
		}

		private void setImportance(final double importance) {
			Mockito.when(this.accountImportance.getImportance(BlockHeight.ONE)).thenReturn(importance);
		}
	}
}
