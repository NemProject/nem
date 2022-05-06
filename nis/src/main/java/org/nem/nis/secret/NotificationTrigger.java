package org.nem.nis.secret;

/**
 * Actions that can trigger notifications.
 */
public enum NotificationTrigger {

	/**
	 * The notification was triggered by an execute action.
	 */
	Execute,

	/**
	 * The notification was triggered by an undo action.
	 */
	Undo
}
