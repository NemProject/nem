package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * Observer that updates the mosaics to which an account is subscribed.
 */
public class AccountInfoMosaicIdsObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 * @param accountStateCache The account state cache.
	 */
	public AccountInfoMosaicIdsObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		switch (notification.getType()) {
			case MosaicDefinitionCreation:
				this.notify((MosaicDefinitionCreationNotification) notification);
				break;

			case MosaicTransfer:
				this.notify((MosaicTransferNotification) notification);
				break;
			default :
				break;
		}
	}

	private void notify(final MosaicDefinitionCreationNotification notification) {
		this.updateMosaicId(notification.getMosaicDefinition().getId(), notification.getMosaicDefinition().getCreator());
	}

	private void notify(final MosaicTransferNotification notification) {
		this.updateMosaicId(notification.getMosaicId(), notification.getSender());
		this.updateMosaicId(notification.getMosaicId(), notification.getRecipient());
	}

	private void updateMosaicId(final MosaicId mosaicId, final Account account) {
		// note that the entry will be null if the mosaic is removed (e.g. an undo of a mosaic creation)
		final MosaicEntry entry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		final AccountInfo info = this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
		if (shouldRemoveMosaicId(entry, account)) {
			info.removeMosaicId(mosaicId);
		} else {
			info.addMosaicId(mosaicId);
		}
	}

	private static boolean shouldRemoveMosaicId(final MosaicEntry entry, final Account account) {
		if (null == entry) {
			return true;
		}

		final boolean hasZeroBalance = entry.getBalances().getBalance(account.getAddress()).equals(Quantity.ZERO);
		final boolean isOwner = entry.getMosaicDefinition().getCreator().equals(account);
		return hasZeroBalance && !isOwner;
	}
}
