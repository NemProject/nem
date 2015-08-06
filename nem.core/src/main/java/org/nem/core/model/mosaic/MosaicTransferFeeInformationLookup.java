package org.nem.core.model.mosaic;

/**
 * An interface for looking up mosaic transfer fee information.
 */
@FunctionalInterface
public interface MosaicTransferFeeInformationLookup {

	/**
	 * Looks up fee information by its id.
	 *
	 * @param id The mosaic id.
	 * @return The fee information associated with the mosaic.
	 */
	MosaicTransferFeeInfo findById(final MosaicId id);
}
