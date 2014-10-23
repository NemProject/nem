package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.*;

/**
 * A block transaction observer that automatically prunes account-related data once every ~30 days.
 */
public class PruningObserver implements BlockTransactionObserver {
	// TODO 20141022 J-*: not sure if this is reasonable or not
	// TODO 20141023 BR -> J: pruning once every 30 days means keeping outlinks/weighted balances for maximal 60 days.
	// TODO                   Since pruning is not expensive I would do it once a day, keep the last 30 days when pruning outlinks
	// TODO                   and probably not more than 2 days when pruning weighted balances (could in theory even be less).
	private static final long PRUNE_BLOCK_MULTIPLER = 30 * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY; // prune once every 30 days
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

		final BlockHeight pruneHeight = new BlockHeight(context.getHeight().getRaw() - PRUNE_BLOCK_MULTIPLER);
		for (final PoiAccountState accountState : this.poiFacade) {
			accountState.getWeightedBalances().prune(pruneHeight);
			accountState.getImportanceInfo().prune(pruneHeight);
		}
	}

	private static boolean shouldPrune(final Notification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() &&
				NotificationType.HarvestReward == notification.getType() &&
				0 == (context.getHeight().getRaw() - 1) % PRUNE_BLOCK_MULTIPLER;
	}
}
