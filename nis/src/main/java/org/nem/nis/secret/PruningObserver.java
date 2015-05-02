package org.nem.nis.secret;

import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;

/**
 * A block transaction observer that automatically prunes account-related data once every 360 blocks.
 */
public class PruningObserver implements BlockTransactionObserver {
	// keep 1 day of weighted balance history, 31 days of outlink history (keep an extra day so that calculations are correct after rollbacks)
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = BlockChainConstants.OUTLINK_HISTORY + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long PRUNE_INTERVAL = 360;
	private final AccountStateCache accountStateCache;
	private final HashCache transactionHashCache;
	private final boolean pruneHistoricalData;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 * @param transactionHashCache The cache of transaction hashes.
	 */
	public PruningObserver(
			final AccountStateCache accountStateCache,
			final HashCache transactionHashCache,
			final boolean pruneHistoricalData) {
		this.accountStateCache = accountStateCache;
		this.transactionHashCache = transactionHashCache;
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
		for (final AccountState accountState : this.accountStateCache.mutableContents()) {
			if (this.pruneHistoricalData) {
				accountState.getWeightedBalances().prune(weightedBalancePruneHeight);
				accountState.getHistoricalImportances().prune();
			}
			accountState.getImportanceInfo().prune(outlinkPruneHeight);
		}

		this.transactionHashCache.prune(context.getTimeStamp());
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
