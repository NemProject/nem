package org.nem.nis.secret;

import org.nem.core.model.SmartTileSupplyType;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Quantity;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;

/**
 * An observer that updates an account's smart tile information.
 */
public class SmartTileSupplyChangeObserver implements BlockTransactionObserver {
	private final AccountStateCache accountStateCache;
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public SmartTileSupplyChangeObserver(final AccountStateCache accountStateCache, final NamespaceCache namespaceCache) {
		this.accountStateCache = accountStateCache;
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
		final AccountState state = this.accountStateCache.findStateByAddress(notification.getSupplier().getAddress());
		final SmartTileMap map = state.getSmartTileMap();
		final MosaicId id = notification.getSmartTile().getMosaicId();
		final NamespaceEntry namespaceEntry = this.namespaceCache.get(id.getNamespaceId());
		final MosaicEntry mosaicEntry = namespaceEntry.getMosaics().get(id);
		final SmartTile smartTile = notification.getSmartTile();
		if ((NotificationTrigger.Execute == context.getTrigger() && notification.getSupplyType().equals(SmartTileSupplyType.CreateSmartTiles)) ||
			(NotificationTrigger.Undo == context.getTrigger() && notification.getSupplyType().equals(SmartTileSupplyType.DeleteSmartTiles))) {
			mosaicEntry.increaseSupply(smartTile.getQuantity());
			map.add(smartTile);
		} else {
			mosaicEntry.decreaseSupply(smartTile.getQuantity());
			final SmartTile newSmartTile = map.subtract(smartTile);

			// note: quantity zero means that either the mosaic has mutable quantity or a transaction was rolled back
			//       and there was no entry in the map before the transaction.
			if (newSmartTile.getQuantity().equals(Quantity.ZERO)) {
				map.remove(newSmartTile.getMosaicId());
			}
		}
	}
}
