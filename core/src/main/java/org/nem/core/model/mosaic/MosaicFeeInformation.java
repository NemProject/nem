package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Supply;

/**
 * Information that is required to calculate the appropriate mosaic transfer fee.
 */
public class MosaicFeeInformation {
	private final Supply supply;
	private final int divisibility;

	/**
	 * Creates a mosaic fee information.
	 *
	 * @param supply The total outstanding supply of the mosaic.
	 * @param divisibility The divisibility.
	 */
	public MosaicFeeInformation(final Supply supply, final int divisibility) {
		this.supply = supply;
		this.divisibility = divisibility;
	}

	/**
	 * Gets the supply.
	 *
	 * @return The supply.
	 */
	public Supply getSupply() {
		return this.supply;
	}

	/**
	 * Gets the divisibility.
	 *
	 * @return The divisibility.
	 */
	public int getDivisibility() {
		return this.divisibility;
	}
}
