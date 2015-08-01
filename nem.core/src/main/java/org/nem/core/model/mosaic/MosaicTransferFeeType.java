package org.nem.core.model.mosaic;

/**
 * Class enumerating all mosaic transfer fee types.
 */
public enum MosaicTransferFeeType {

	/**
	 * Fee represents an absolute value.
	 */
	Absolute(1),

	/**
	 * Fee is proportional to a percentile of the transferred mosaic.
	 */
	Percentile(2);

	private final int value;

	MosaicTransferFeeType(final int value) {
		this.value = value;
	}

	/**
	 * Creates a type given a raw value.
	 *
	 * @param value The value.
	 * @return The type if the value is known.
	 */
	public static MosaicTransferFeeType fromValue(final int value) {
		for (final MosaicTransferFeeType type : values()) {
			if (type.value == value) {
				return type;
			}
		}

		throw new IllegalArgumentException("Invalid mosaic transfer fee type: " + value);
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
