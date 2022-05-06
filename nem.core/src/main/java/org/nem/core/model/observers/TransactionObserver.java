package org.nem.core.model.observers;

/**
 * An observer that notifies listeners when transactions are made.
 */
public interface TransactionObserver extends NamedObserver {

	/**
	 * A notification event has been raised.
	 *
	 * @param notification The notification event arguments.
	 */
	void notify(final Notification notification);
}
