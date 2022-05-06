package org.nem.core.model.observers;

/**
 * A notification.
 */
public abstract class Notification {
	private final NotificationType type;

	/**
	 * Creates a notification.
	 *
	 * @param type The notification type.
	 */
	public Notification(final NotificationType type) {
		this.type = type;
	}

	/**
	 * Gets the notification type.
	 *
	 * @return The notification type.
	 */
	public NotificationType getType() {
		return this.type;
	}
}
