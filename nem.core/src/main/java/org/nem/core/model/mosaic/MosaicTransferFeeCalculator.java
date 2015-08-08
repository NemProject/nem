package org.nem.core.model.mosaic;

/**
 * Interface for calculating mosaic transfer fees.
 */
@FunctionalInterface
public interface MosaicTransferFeeCalculator {

	/**
	 * Calculates the absolute levy for the specified mosaic transfer.
	 *
	 * @param mosaic The mosaic.
	 * @return The absolute levy.
	 */
	MosaicLevy calculateAbsoluteLevy(final Mosaic mosaic);
}
