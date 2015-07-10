package org.nem.core.model.observers;

import org.nem.core.model.SmartTileSupplyType;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

/**
 * A notification that represents a supply change for a smart tile type.
 */
public class SmartTileSupplyChangeNotification extends Notification {
	private final MosaicId mosaicId;
	private final SmartTileSupplyType supplyType;
	private final Quantity quantity;

	/**
	 * Creates a new smart tile supply change notification.
	 *
	 * @param mosaicId The mosaic id.
	 * @param supplyType The supply type.
	 * @param quantity The quantity.
	 */
	public SmartTileSupplyChangeNotification(
			final MosaicId mosaicId,
			final SmartTileSupplyType supplyType,
			final Quantity quantity) {
		super(NotificationType.SmartTileSupplyChange);
		this.mosaicId = mosaicId;
		this.supplyType = supplyType;
		this.quantity = quantity;
	}

	/**
	 * Gets the mosaic id.
	 *
	 * @return The mosaic id.
	 */
	public MosaicId getMosaicId() {
		return this.mosaicId;
	}

	/**
	 * Gets the supply type.
	 *
	 * @return The supply type.
	 */
	public SmartTileSupplyType getSupplyType() {
		return this.supplyType;
	}

	/**
	 * Gets the quantity.
	 *
	 * @return The quantity.
	 */
	public Quantity getQuantity() {
		return this.quantity;
	}
}
