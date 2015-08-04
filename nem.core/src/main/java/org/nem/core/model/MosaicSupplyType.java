package org.nem.core.model;

/**
 * Enum containing mosaic supply types.
 */
public enum MosaicSupplyType {
	/**
	 * An unknown mode.
	 */
	Unknown(0),

	/**
	 * Create new mosaics.
	 */
	Create(1),

	/**
	 * Delete existing mosaics.
	 */
	Delete(2);

	private final int value;

	MosaicSupplyType(final int value) {
		this.value = value;
	}

	/**
	 * Gets a value indicating whether or not this type is valid.
	 *
	 * @return true if this type is valid, false otherwise.
	 */
	public boolean isValid() {
		switch (this) {
			case Create:
			case Delete:
				return true;
		}

		return false;
	}

	/**
	 * Creates a supply type given a raw value.
	 *
	 * @param value The value.
	 * @return The mode if the value is known or Unknown if it was not.
	 */
	public static MosaicSupplyType fromValueOrDefault(final int value) {
		for (final MosaicSupplyType supplyType : values()) {
			if (supplyType.value() == value) {
				return supplyType;
			}
		}

		return MosaicSupplyType.Unknown;
	}

	/**
	 * Gets the underlying integer representation of the supply type.
	 *
	 * @return The underlying value.
	 */
	public int value() {
		return this.value;
	}
}
