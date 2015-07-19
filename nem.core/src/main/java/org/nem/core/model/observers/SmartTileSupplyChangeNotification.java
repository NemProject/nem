package org.nem.core.model.observers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.SmartTile;

/**
 * A notification that represents a supply change for a smart tile type.
 */
public class SmartTileSupplyChangeNotification extends Notification {
	private final Account supplier;
	private final SmartTile smartTile;
	private final SmartTileSupplyType supplyType;

	/**
	 * Creates a new smart tile supply change notification.
	 *
	 * @param supplier The supplier.
	 * @param smartTile The smart tile.
	 * @param supplyType The supply type.
	 */
	public SmartTileSupplyChangeNotification(
			final Account supplier,
			final SmartTile smartTile,
			final SmartTileSupplyType supplyType) {
		super(NotificationType.SmartTileSupplyChange);
		this.supplier = supplier;
		this.smartTile = smartTile;
		this.supplyType = supplyType;
	}

	/**
	 * Gets the supplier.
	 *
	 * @return The supplier.
	 */
	public Account getSupplier() {
		return this.supplier;
	}

	/**
	 * Gets the smart tile.
	 *
	 * @return The smart tile.
	 */
	public SmartTile getSmartTile() {
		return this.smartTile;
	}

	/**
	 * Gets the supply type.
	 *
	 * @return The supply type.
	 */
	public SmartTileSupplyType getSupplyType() {
		return this.supplyType;
	}
}
