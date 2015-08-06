package org.nem.core.model.mosaic;

import org.nem.core.model.primitive.Supply;

/**
 * Information that is required to calculate the appropriate mosaic transfer fee.
 */
public class MosaicFeeInformation {
	private final Supply supply;
	private final int divisibility;
	private final MosaicTransferFeeInfo transferFeeInfo;

	/**
	 * Creates a mosaic fee information.
	 *
	 * @param supply The total outstanding supply of the mosaic.
	 * @param divisibility The divisibility.
	 * @param transferFeeInfo The transfer fee info.
	 */
	public MosaicFeeInformation(final Supply supply, final int divisibility, final MosaicTransferFeeInfo transferFeeInfo) {
		this.supply = supply;
		this.divisibility = divisibility;
		this.transferFeeInfo = transferFeeInfo;
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

	/**
	 * Gets the transfer fee info.
	 *
	 * @return The transfer fee info.
	 */
	public MosaicTransferFeeInfo getTransferFeeInfo() {
		return this.transferFeeInfo;
	}
}
