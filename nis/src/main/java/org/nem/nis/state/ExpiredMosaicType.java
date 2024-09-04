package org.nem.nis.state;

/**
 * Enum containing types of expired mosaics.
 */
public enum ExpiredMosaicType {

	/**
	 * Mosaic has expired.
	 */
	Expired(1),

	/**
	 * Mosaic has been restored.
	 */
	Restored(2);

	private final int value;

	ExpiredMosaicType(final int value) {
		this.value = value;
	}

	/**
	 * Gets the underlying integer representation of the type.
	 *
	 * @return The underlying value.
	 */
	public int value() {
		return this.value;
	}
}
