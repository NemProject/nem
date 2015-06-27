package org.nem.nis.secret;

import org.nem.core.model.observers.*;

/**
 * An observer that notifies listeners when transactions are made.
 */
public interface BlockTransactionObserver extends NamedObserver {

	/**
	 * A notification event has been raised.
	 *
	 * @param notification The notification event arguments.
	 * @param context The notification context.
	 */
	void notify(final Notification notification, final BlockNotificationContext context);
}
