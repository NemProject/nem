package org.nem.core.model.observers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Supply;

/**
 * A notification that represents a supply change for a mosaic.
 */
public class MosaicSupplyChangeNotification extends Notification {
	private final Account supplier;
	private final MosaicId mosaicId;
	private final Supply delta;
	private final MosaicSupplyType supplyType;

	/**
	 * Creates a new mosaic supply change notification.
	 *
	 * @param supplier The supplier.
	 * @param mosaicId The mosaic id.
	 * @param delta The supply change quantity.
	 * @param supplyType The supply type.
	 */
	public MosaicSupplyChangeNotification(
			final Account supplier,
			final MosaicId mosaicId,
			final Supply delta,
			final MosaicSupplyType supplyType) {
		super(NotificationType.MosaicSupplyChange);
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
	public Supply getDelta() {
		return this.delta;
	}

	/**
	 * Gets the supply type.
	 *
	 * @return The supply type.
	 */
	public MosaicSupplyType getSupplyType() {
		return this.supplyType;
	}
}
