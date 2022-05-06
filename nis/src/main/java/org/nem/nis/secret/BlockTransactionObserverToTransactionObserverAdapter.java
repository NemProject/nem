package org.nem.nis.secret;

import org.nem.core.model.observers.*;

/**
 * An observer that implements TransactionObserver by forwarding all transfer notifications to the wrapped BlockTransactionObserver
 * implementation.
 */
public class BlockTransactionObserverToTransactionObserverAdapter implements TransactionObserver {
	private final BlockTransactionObserver observer;
	private final BlockNotificationContext context;

	/**
	 * Creates a new adapter.
	 *
	 * @param observer The wrapped block transaction observer.
	 * @param context The supplementary context.
	 */
	public BlockTransactionObserverToTransactionObserverAdapter(final BlockTransactionObserver observer,
			final BlockNotificationContext context) {
		this.observer = observer;
		this.context = context;
	}

	@Override
	public final void notify(final Notification notification) {
		this.observer.notify(notification, this.context);
	}
}
