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
public class MosaicTransferObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicTransferObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MosaicTransfer) {
			return;
		}

		this.notify((MosaicTransferNotification) notification);
	}

	private void notify(final MosaicTransferNotification notification) {
		final MosaicId mosaicId = notification.getMosaicId();
		final MosaicEntry entry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		final MosaicBalances balances = entry.getBalances();

		final Address senderAddress = notification.getSender().getAddress();
		final Address recipientAddress = notification.getRecipient().getAddress();
		final Quantity quantity = notification.getQuantity();

		// note: sender and recipient already have been swapped by ReverseTransactionObserver in case of undoing a mosaic transfer
		balances.decrementBalance(senderAddress, quantity);
		balances.incrementBalance(recipientAddress, quantity);
	}
}
