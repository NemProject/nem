package org.nem.nis.secret;

import org.nem.core.model.observers.Notification;

/**
 * An observer that notifies listeners when transactions are made.
 */
public interface BlockTransactionObserver {

	/**
	 * A notification event has been raised.
	 *
	 * @param notification The notification event arguments.
	 * @param context The notification context.
	 */
	public void notify(final Notification notification, final BlockNotificationContext context);
}
