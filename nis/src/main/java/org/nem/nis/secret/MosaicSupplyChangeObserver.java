package org.nem.nis.secret;

import org.nem.core.model.MosaicSupplyType;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Supply;
import org.nem.nis.cache.NamespaceCache;
import org.nem.nis.state.MosaicEntry;

/**
 * An observer that updates a mosaic's supply.
 */
public class MosaicSupplyChangeObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public MosaicSupplyChangeObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.MosaicSupplyChange) {
			return;
		}

		this.notify((MosaicSupplyChangeNotification)notification, context);
	}

	private void notify(final MosaicSupplyChangeNotification notification, final BlockNotificationContext context) {
		final MosaicId mosaicId = notification.getMosaicId();
		final Supply delta = notification.getDelta();

		final MosaicEntry mosaicEntry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		if (shouldIncrease(notification, context)) {
			mosaicEntry.increaseSupply(delta);
		} else {
			mosaicEntry.decreaseSupply(delta);
		}
	}

	private static boolean shouldIncrease(final MosaicSupplyChangeNotification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() && notification.getSupplyType().equals(MosaicSupplyType.Create)
				|| NotificationTrigger.Undo == context.getTrigger() && notification.getSupplyType().equals(MosaicSupplyType.Delete);
	}
}
