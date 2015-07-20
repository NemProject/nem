package org.nem.nis.secret;

import org.nem.core.model.SmartTileSupplyType;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * An observer that updates a smart tile's supply.
 */
public class SmartTileSupplyChangeObserver implements BlockTransactionObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public SmartTileSupplyChangeObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.SmartTileSupplyChange) {
			return;
		}

		this.notify((SmartTileSupplyChangeNotification)notification, context);
	}

	private void notify(final SmartTileSupplyChangeNotification notification, final BlockNotificationContext context) {
		final MosaicId mosaicId = notification.getSmartTile().getMosaicId();
		final Quantity delta = notification.getSmartTile().getQuantity();

		final MosaicEntry mosaicEntry = this.namespaceCache.get(mosaicId.getNamespaceId()).getMosaics().get(mosaicId);
		if (shouldIncrease(notification, context)) {
			mosaicEntry.increaseSupply(delta);
		} else {
			mosaicEntry.decreaseSupply(delta);
		}
	}

	private static boolean shouldIncrease(final SmartTileSupplyChangeNotification notification, final BlockNotificationContext context) {
		return NotificationTrigger.Execute == context.getTrigger() && notification.getSupplyType().equals(SmartTileSupplyType.CreateSmartTiles)
				|| NotificationTrigger.Undo == context.getTrigger() && notification.getSupplyType().equals(SmartTileSupplyType.DeleteSmartTiles);
	}
}
