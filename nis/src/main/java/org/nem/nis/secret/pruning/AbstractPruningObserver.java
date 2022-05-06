package org.nem.nis.secret.pruning;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.secret.*;

/**
 * A base class for pruning block transaction observers that automatically prunes cached data once every 360 blocks.
 */
public abstract class AbstractPruningObserver implements BlockTransactionObserver {
	private static final long PRUNE_INTERVAL = 360;

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (!shouldPrune(notification, context)) {
			return;
		}

		this.prune(context);
	}

	/**
	 * Triggers the pruning operation.
	 *
	 * @param context The block notification context.
	 */
	protected abstract void prune(final BlockNotificationContext context);

	private static boolean shouldPrune(final Notification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() && NotificationType.BlockHarvest == notification.getType()
				&& 1 == (context.getHeight().getRaw() % PRUNE_INTERVAL);
	}

	/**
	 * Gets the prune block height.
	 *
	 * @param height The current height.
	 * @param numHistoryBlocks The number of history blocks to keep.
	 * @return The prune block height.
	 */
	protected static BlockHeight getPruneHeight(final BlockHeight height, final long numHistoryBlocks) {
		return new BlockHeight(Math.max(1, height.getRaw() - numHistoryBlocks));
	}
}
