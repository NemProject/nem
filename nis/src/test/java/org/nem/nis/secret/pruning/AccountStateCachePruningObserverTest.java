package org.nem.nis.secret.pruning;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.NisCache;
import org.nem.nis.secret.BlockTransactionObserver;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisTestConstants;

public abstract class AccountStateCachePruningObserverTest extends AbstractPruningObserverTest {
	private static final int WEIGHTED_BALANCE_BLOCK_HISTORY = NisTestConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final int OUTLINK_BLOCK_HISTORY = NisTestConstants.ESTIMATED_BLOCKS_PER_MONTH
			+ NisTestConstants.ESTIMATED_BLOCKS_PER_DAY;

	// region overrides

	protected abstract boolean pruneHistoricalData();

	@Override
	protected BlockTransactionObserver createObserver(final NisCache nisCache) {
		return new AccountStateCachePruningObserver(nisCache.getAccountStateCache(), this.pruneHistoricalData());
	}

	@Override
	protected void assertPruning(final NisCache nisCache, final long state) {
		final int weightedBalancePruneHeight = (int) (state >> 32);
		final int outlinkPruneHeight = (int) state;

		// Assert:
		if (0 != weightedBalancePruneHeight && this.pruneHistoricalData()) {
			this.assertWeightedBalancePruning(nisCache, new BlockHeight(weightedBalancePruneHeight));
		} else {
			this.assertNoWeightedBalancePruning(nisCache);
		}

		if (0 != outlinkPruneHeight) {
			this.assertOutlinkPruning(nisCache, new BlockHeight(outlinkPruneHeight));
		} else {
			this.assertNoOutlinkPruning(nisCache);
		}

		// historical importances and weighted balances are pruned with the same frequency
		if (0 != weightedBalancePruneHeight && this.pruneHistoricalData()) {
			this.assertHistoricalImportancePruning(nisCache);
		} else {
			this.assertNoHistoricalImportancePruning(nisCache);
		}
	}

	@Override
	protected void assertNoPruning(final NisCache nisCache) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getWeightedBalances(), Mockito.never()).prune(Mockito.any());
			Mockito.verify(accountState.getImportanceInfo(), Mockito.never()).prune(Mockito.any());
			Mockito.verify(accountState.getHistoricalImportances(), Mockito.never()).prune();
		}
	}

	// endregion

	// region helper functions

	private void assertNoWeightedBalancePruning(final NisCache nisCache) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getWeightedBalances(), Mockito.never()).prune(Mockito.any());
		}
	}

	private void assertNoOutlinkPruning(final NisCache nisCache) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getImportanceInfo(), Mockito.never()).prune(Mockito.any());
		}
	}

	private void assertNoHistoricalImportancePruning(final NisCache nisCache) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getHistoricalImportances(), Mockito.never()).prune();
		}
	}

	private void assertWeightedBalancePruning(final NisCache nisCache, final BlockHeight height) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getWeightedBalances(), Mockito.only()).prune(height);
		}
	}

	private void assertOutlinkPruning(final NisCache nisCache, final BlockHeight height) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getImportanceInfo(), Mockito.only()).prune(height);
		}
	}

	private void assertHistoricalImportancePruning(final NisCache nisCache) {
		for (final AccountState accountState : nisCache.getAccountStateCache().mutableContents()) {
			Mockito.verify(accountState.getHistoricalImportances(), Mockito.only()).prune();
		}
	}

	// endregion

	@Test
	public void blockBasedPruningIsTriggeredAtInitialBlockHeight() {
		// Assert:
		this.assertBlockBasedPruning(1, createState(1, 1));
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearWeightedBalanceBlockHistory() {
		// Assert:
		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY, createState(0, 0));
		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 1, createState(1, 1));

		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL, createState(0, 0));
		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 1, createState(361, 1));
		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + PRUNE_INTERVAL + 2, createState(0, 0));

		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL, createState(0, 0));
		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 1, createState(721, 1));
		this.assertBlockBasedPruning(WEIGHTED_BALANCE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 2, createState(0, 0));
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearOutlinkBlockHistory() {
		// Arrange:
		final int outlinkHistory = OUTLINK_BLOCK_HISTORY;
		final int historyDifference = OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY;

		// Assert:
		this.assertBlockBasedPruning(outlinkHistory, createState(0, 0));
		this.assertBlockBasedPruning(outlinkHistory + 1, createState(historyDifference + 1, 1));

		this.assertBlockBasedPruning(outlinkHistory + PRUNE_INTERVAL, createState(0, 0));
		this.assertBlockBasedPruning(outlinkHistory + PRUNE_INTERVAL + 1, createState(historyDifference + 361, 361));
		this.assertBlockBasedPruning(outlinkHistory + PRUNE_INTERVAL + 2, createState(0, 0));

		this.assertBlockBasedPruning(outlinkHistory + 2 * PRUNE_INTERVAL, createState(0, 0));
		this.assertBlockBasedPruning(outlinkHistory + 2 * PRUNE_INTERVAL + 1, createState(historyDifference + 721, 721));
		this.assertBlockBasedPruning(outlinkHistory + 2 * PRUNE_INTERVAL + 2, createState(0, 0));
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsMuchGreaterThanHistories() {
		// Assert:
		this.assertBlockBasedPruning(100 * OUTLINK_BLOCK_HISTORY + 1,
				createState(100 * OUTLINK_BLOCK_HISTORY - WEIGHTED_BALANCE_BLOCK_HISTORY + 1, 99 * OUTLINK_BLOCK_HISTORY + 1));
	}

	@Test
	public void outlinkPruningUsesOutlinkBlockHistory() {
		// Assert:
		final int notificationHeight = 2345 * 360 + 1;
		this.assertBlockBasedPruning(notificationHeight,
				createState(notificationHeight - WEIGHTED_BALANCE_BLOCK_HISTORY, notificationHeight - OUTLINK_BLOCK_HISTORY));
	}

	private static long createState(final int weightedBalancePruneHeight, final int outlinkPruneHeight) {
		return ((long) weightedBalancePruneHeight) << 32 | outlinkPruneHeight;
	}

	public static class AccountStateCachePruningObserverTestWithHistoricalDataPruning extends AccountStateCachePruningObserverTest {

		@Override
		protected boolean pruneHistoricalData() {
			return true;
		}
	}

	public static class AccountStateCachePruningObserverTestWithoutHistoricalDataPruning extends AccountStateCachePruningObserverTest {

		@Override
		protected boolean pruneHistoricalData() {
			return false;
		}
	}
}
