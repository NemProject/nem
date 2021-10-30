package org.nem.nis.secret;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

/**
 * Contextual information associated with a block notification.
 */
public class BlockNotificationContext {
	private final BlockHeight height;
	private final TimeInstant timeStamp;
	private final NotificationTrigger trigger;

	/**
	 * Creates a new context.
	 *
	 * @param height The block height.
	 * @param timeStamp The block timestamp.
	 * @param trigger The trigger.
	 */
	public BlockNotificationContext(final BlockHeight height, final TimeInstant timeStamp, final NotificationTrigger trigger) {
		this.height = height;
		this.timeStamp = timeStamp;
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
	 * Gets the block time stamp.
	 *
	 * @return The block time stamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
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
