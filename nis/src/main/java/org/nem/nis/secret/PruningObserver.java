package org.nem.nis.secret;

import org.nem.nis.cache.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.state.AccountState;

/**
 * A block transaction observer that automatically prunes account-related data once every 360 blocks.
 */
public class PruningObserver implements BlockTransactionObserver {
	// keep 1 day of weighted balance history, 31 days of outlink history (keep an extra day so that calculations are correct after rollbacks)
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = BlockChainConstants.OUTLINK_HISTORY + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY_OLD = BlockChainConstants.OUTLINK_HISTORY;
	private static final long PRUNE_INTERVAL = 360;
	private final AccountStateCache accountStateCache;
	private final HashCache transactionHashCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public PruningObserver(final AccountStateCache accountStateCache, final HashCache transactionHashCache) {
		this.accountStateCache = accountStateCache;
		this.transactionHashCache = transactionHashCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (!shouldPrune(notification, context)) {
			return;
		}

		final BlockHeight weightedBalancePruneHeight = getPruneHeight(context.getHeight(), WEIGHTED_BALANCE_BLOCK_HISTORY);
		final long outlinkBlockHistory = BlockMarkerConstants.BETA_OUTLINK_PRUNING_FORK <= context.getHeight().getRaw()
				? OUTLINK_BLOCK_HISTORY
				: OUTLINK_BLOCK_HISTORY_OLD;
		final BlockHeight outlinkPruneHeight = getPruneHeight(context.getHeight(), outlinkBlockHistory);
		for (final AccountState accountState : this.accountStateCache) {
			accountState.getWeightedBalances().prune(weightedBalancePruneHeight);
			accountState.getImportanceInfo().prune(outlinkPruneHeight);
		}

		final TimeInstant pruneTime = getPruneTime(context.getTimeStamp(), this.transactionHashCache.getRetentionTime());
		this.transactionHashCache.prune(pruneTime);
	}

	private static BlockHeight getPruneHeight(final BlockHeight height, final long numHistoryBlocks) {
		return new BlockHeight(Math.max(1, height.getRaw() - numHistoryBlocks));
	}

	private static TimeInstant getPruneTime(final TimeInstant currentTime, final int retentionHours) {
		final TimeInstant retentionTime = TimeInstant.ZERO.addHours(retentionHours);
		return new TimeInstant(currentTime.compareTo(retentionTime) <= 0 ? 0 : currentTime.subtract(retentionTime));
	}

	private static boolean shouldPrune(final Notification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() &&
				NotificationType.HarvestReward == notification.getType() &&
				1 == (context.getHeight().getRaw() % PRUNE_INTERVAL);
	}
}
