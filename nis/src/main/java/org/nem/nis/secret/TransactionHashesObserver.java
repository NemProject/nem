package org.nem.nis.secret;

import org.nem.core.model.HashMetaDataPair;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.HashCache;

import java.util.stream.Collectors;

/**
 * BlockTransactionObserver that updates the transaction hash cache.
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

		this.notify((TransactionHashesNotification) notification, context);
	}

	public void notify(final TransactionHashesNotification notification, final BlockNotificationContext context) {
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.transactionHashCache.putAll(notification.getPairs());
		} else {
			this.transactionHashCache
					.removeAll(notification.getPairs().stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));
		}
	}
}
