package org.nem.nis.secret;

import org.nem.core.model.observers.*;

/**
 * An observer that implements BlockTransactionObserver by forwarding all transfer notifications to the wrapped TransactionObserver
 * implementation.
 */
public class TransactionObserverToBlockTransferObserverAdapter implements BlockTransactionObserver {
	private final TransactionObserver observer;

	/**
	 * Creates a new adapter.
	 *
	 * @param observer The wrapped transaction observer.
	 */
	public TransactionObserverToBlockTransferObserverAdapter(final TransactionObserver observer) {
		this.observer = observer;
	}

	@Override
	public String getName() {
		return this.observer.getName();
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		this.observer.notify(notification);
	}
}
