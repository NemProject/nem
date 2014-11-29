package org.nem.nis.secret;

import org.nem.core.model.HashCache;
import org.nem.core.model.observers.*;

/**
 *  BlockTransactionObserver that updates the transaction hash cache.
 */
public class TransactionHashesObserver implements BlockTransactionObserver {
	private final HashCache transactionHashCache;

	/**
	 * Creates a new observer.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 */
	public TransactionHashesObserver(final HashCache transactionHashCache) {
		this.transactionHashCache = transactionHashCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.TransactionHashes != notification.getType()) {
			return;
		}

		this.notify((TransactionHashesNotification)notification, context);
	}

	public void notify(final TransactionHashesNotification notification, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.transactionHashCache.putAll(notification.getHashes(), notification.getTimeStamps());
		} else {
			this.transactionHashCache.removeAll(notification.getHashes());
		}
	}
}
