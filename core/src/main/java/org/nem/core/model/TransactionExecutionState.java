package org.nem.core.model;

import org.nem.core.model.mosaic.MosaicTransferFeeCalculator;

/**
 * Additional state provided to transaction execute / undo operations.
 */
public class TransactionExecutionState {
	private final MosaicTransferFeeCalculator mosaicTransferFeeCalculator;

	/**
	 * Creates transaction execution state.
	 *
	 * @param mosaicTransferFeeCalculator The mosaic transfer fee calculator.
	 */
	public TransactionExecutionState(final MosaicTransferFeeCalculator mosaicTransferFeeCalculator) {
		this.mosaicTransferFeeCalculator = mosaicTransferFeeCalculator;
	}

	/**
	 * Gets the global mosaic transfer fee calculator.
	 *
	 * @return The mosaic transfer fee calculator.
	 */
	public MosaicTransferFeeCalculator getMosaicTransferFeeCalculator() {
		return this.mosaicTransferFeeCalculator;
	}
}
