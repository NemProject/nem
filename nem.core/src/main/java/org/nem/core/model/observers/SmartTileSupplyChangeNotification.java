package org.nem.core.model.observers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.Quantity;

/**
 * A notification that represents a supply change for a smart tile type.
 */
public class SmartTileSupplyChangeNotification extends Notification {
	private final Account supplier;
	private final MosaicId mosaicId;
	private final Quantity delta;
	private final SmartTileSupplyType supplyType;

	/**
	 * Creates a new smart tile supply change notification.
	 *
	 * @param supplier The supplier.
	 * @param mosaicId The mosaic id.
	 * @param delta The supply change quantity.
	 * @param supplyType The supply type.
	 */
	public SmartTileSupplyChangeNotification(
			final Account supplier,
			final MosaicId mosaicId,
			final Quantity delta,
			final SmartTileSupplyType supplyType) {
		super(NotificationType.SmartTileSupplyChange);
		this.supplier = supplier;
		this.mosaicId = mosaicId;
		this.delta = delta;
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
	 * Gets the mosaic id.
	 *
	 * @return The mosaic id.
	 */
	public MosaicId getMosaicId() {
		return this.mosaicId;
	}

	/**
	 * Gets the supply change quantity.
	 *
	 * @return The supply change quantity.
	 */
	public Quantity getDelta() {
		return this.delta;
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
