package org.nem.nis.secret;

import org.nem.core.model.Address;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.NamespaceCache;
import org.nem.nis.state.*;

/**
 * Observer that commits mosaic balance changes to the underlying accounts.
 */
public class SmartTileTransferObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public SmartTileTransferObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.SmartTileTransfer) {
			return;
		}

		this.notify((SmartTileTransferNotification)notification, context.getTrigger());
	}

	private void notify(final SmartTileTransferNotification notification, final NotificationTrigger trigger) {
		final MosaicId mosaicId = notification.getMosaicId();
		final MosaicEntry entry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		final MosaicBalances balances = entry.getBalances();

		final Address senderAddress = notification.getSender().getAddress();
		final Address recipientAddress = notification.getRecipient().getAddress();
		final Quantity quantity = notification.getQuantity();
		switch (trigger) {
			case Execute:
				balances.decrementBalance(senderAddress, quantity);
				balances.incrementBalance(recipientAddress, quantity);
				break;

			case Undo:
				balances.decrementBalance(recipientAddress, quantity);
				balances.incrementBalance(senderAddress, quantity);
		}
	}
}
