package org.nem.core.model;

/**
 * Enum containing smart tile supply types.
 */
public enum SmartTileSupplyType {
	/**
	 * An unknown mode.
	 */
	Unknown(0),

	/**
	 * Create new smart tiles.
	 */
	CreateSmartTiles(1),

	/**
	 * Delete new smart tiles.
	 */
	DeleteSmartTiles(2);

	private final int value;

	SmartTileSupplyType(final int value) {
		this.value = value;
	}

	/**
	 * Gets a value indicating whether or not this type is valid.
	 *
	 * @return true if this type is valid, false otherwise.
	 */
	public boolean isValid() {
		switch (this) {
			case CreateSmartTiles:
			case DeleteSmartTiles:
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
	public static SmartTileSupplyType fromValueOrDefault(final int value) {
		for (final SmartTileSupplyType supplyType : values()) {
			if (supplyType.value() == value) {
				return supplyType;
			}
		}

		return SmartTileSupplyType.Unknown;
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
