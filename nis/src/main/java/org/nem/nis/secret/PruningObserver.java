package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.poi.*;

/**
 * A block transaction observer that automatically prunes account-related data once every ~30 days.
 */
public class PruningObserver implements BlockTransactionObserver {
	// keep 2 days of weighted balance history and 30 days of outlink history
	private static final long WEIGHTED_BALANCE_BLOCK_HISTORY = 2 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private static final long OUTLINK_BLOCK_HISTORY = 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	private final PoiFacade poiFacade;

	/**
	 * Creates a new observer.
	 *
	 * @param poiFacade The poi facade.
	 */
	public PruningObserver(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (!shouldPrune(notification, context)) {
			return;
		}

		final BlockHeight weightedBalancePruneHeight = getPruneHeight(context.getHeight(), WEIGHTED_BALANCE_BLOCK_HISTORY);
		final BlockHeight outlinkPruneHeight = getPruneHeight(context.getHeight(), OUTLINK_BLOCK_HISTORY);
		for (final PoiAccountState accountState : this.poiFacade) {
			accountState.getWeightedBalances().prune(weightedBalancePruneHeight);
			accountState.getImportanceInfo().prune(outlinkPruneHeight);
		}
	}

	private static BlockHeight getPruneHeight(final BlockHeight height, final long numHistoryBlocks) {
		return new BlockHeight(Math.max(1, height.getRaw() - numHistoryBlocks));
	}

	private static boolean shouldPrune(final Notification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() &&
				NotificationType.HarvestReward == notification.getType();
	}
}
