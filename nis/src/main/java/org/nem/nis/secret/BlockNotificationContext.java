package org.nem.nis.secret;

import org.nem.core.model.primitive.BlockHeight;

/**
 * Contextual information associated with a block notification.
 */
public class BlockNotificationContext {
	private final BlockHeight height;
	private final NotificationTrigger trigger;

	/**
	 * Creates a new context.
	 *
	 * @param height The block height.
	 * @param trigger The trigger.
	 */
	public BlockNotificationContext(
			final BlockHeight height,
			final NotificationTrigger trigger) {
		this.height = height;
		this.trigger = trigger;
	}

	/**
	 * Gets the block height.
	 *
	 * @return The block height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets the trigger.
	 *
	 * @return The trigger.
	 */
	public NotificationTrigger getTrigger() {
		return this.trigger;
	}
}
