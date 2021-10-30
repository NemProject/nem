package org.nem.nis.secret;

import org.nem.core.model.MosaicSupplyType;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * An observer that updates a mosaic's supply.
 */
public class MosaicSupplyChangeObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 * @param accountStateCache The account state cache.
	 */
	public MosaicSupplyChangeObserver(final NamespaceCache namespaceCache, final AccountStateCache accountStateCache) {
		this.namespaceCache = namespaceCache;
		this.accountStateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MosaicSupplyChange) {
			return;
		}

		this.notify((MosaicSupplyChangeNotification) notification, context);
	}

	private void notify(final MosaicSupplyChangeNotification notification, final BlockNotificationContext context) {
		final MosaicId mosaicId = notification.getMosaicId();
		final Supply delta = notification.getDelta();

		final MosaicEntry mosaicEntry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		if (shouldIncrease(notification, context)) {
			mosaicEntry.increaseSupply(delta);
		} else {
			mosaicEntry.decreaseSupply(delta);
			if (mosaicEntry.getBalances().getBalance(notification.getSupplier().getAddress()).equals(Quantity.ZERO)) {
				final AccountState accountState = this.accountStateCache.findStateByAddress(notification.getSupplier().getAddress());
				accountState.getAccountInfo().getMosaicIds().remove(notification.getMosaicId());
			}
		}
	}

	private static boolean shouldIncrease(final MosaicSupplyChangeNotification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() && notification.getSupplyType().equals(MosaicSupplyType.Create)
				|| NotificationTrigger.Undo == context.getTrigger() && notification.getSupplyType().equals(MosaicSupplyType.Delete);
	}
}
