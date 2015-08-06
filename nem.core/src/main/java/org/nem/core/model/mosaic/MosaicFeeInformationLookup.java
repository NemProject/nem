package org.nem.core.model.mosaic;

/**
 * An interface for looking up mosaic fee information.
 */
@FunctionalInterface
public interface MosaicFeeInformationLookup {

	/**
	 * Looks up fee information by its id.
	 *
	 * @param id The mosaic id.
	 * @return The fee information associated with the mosaic.
	 */
	MosaicFeeInformation findById(final MosaicId id);
}
