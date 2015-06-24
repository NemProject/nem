package org.nem.nis.secret;

import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.NisCache;
import org.nem.nis.state.AccountState;

/**
 * A block transaction observer that automatically prunes account-related data once every 360 blocks.
 */
public class PruningObserver implements BlockTransactionObserver {
	// keep 1 day of weighted balance history, 31 days of outlink history (keep an extra day so that calculations are correct after rollbacks)
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = BlockChainConstants.OUTLINK_HISTORY + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long NAMESPACE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY * (365 + 30 + 1);
	private static final long PRUNE_INTERVAL = 360;
	private final NisCache nisCache;
	private final boolean pruneHistoricalData;

	/**
	 * Creates a new observer.
	 *
	 * @param nisCache The nis cache.
	 * @param pruneHistoricalData Flag indicating if historical data should be pruned.
	 */
	public PruningObserver(
			final NisCache nisCache,
			final boolean pruneHistoricalData) {
		this.nisCache = nisCache;
		this.pruneHistoricalData = pruneHistoricalData;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (!shouldPrune(notification, context)) {
			return;
		}

		final BlockHeight weightedBalancePruneHeight = getPruneHeight(context.getHeight(), WEIGHTED_BALANCE_BLOCK_HISTORY);
		final long outlinkBlockHistory = OUTLINK_BLOCK_HISTORY;
		final BlockHeight outlinkPruneHeight = getPruneHeight(context.getHeight(), outlinkBlockHistory);
		for (final AccountState accountState : this.nisCache.getAccountStateCache().mutableContents()) {
			if (this.pruneHistoricalData) {
				accountState.getWeightedBalances().prune(weightedBalancePruneHeight);
				accountState.getHistoricalImportances().prune();
			}
			accountState.getImportanceInfo().prune(outlinkPruneHeight);
		}

		this.nisCache.getTransactionHashCache().prune(context.getTimeStamp());
		final Long namespacePruneHeight = Math.max(1, context.getHeight().getRaw() - NAMESPACE_BLOCK_HISTORY);
		this.nisCache.getNamespaceCache().prune(new BlockHeight(namespacePruneHeight));
	}

	private static BlockHeight getPruneHeight(final BlockHeight height, final long numHistoryBlocks) {
		return new BlockHeight(Math.max(1, height.getRaw() - numHistoryBlocks));
	}

	private static boolean shouldPrune(final Notification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() &&
				NotificationType.BlockHarvest == notification.getType() &&
				1 == (context.getHeight().getRaw() % PRUNE_INTERVAL);
	}
}
