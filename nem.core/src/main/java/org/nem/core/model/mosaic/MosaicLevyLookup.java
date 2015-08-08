package org.nem.core.model.mosaic;

/**
 * An interface for looking up mosaic levy information.
 */
@FunctionalInterface
public interface MosaicLevyLookup {

	/**
	 * Looks up mosaic levy information by mosaic id.
	 *
	 * @param id The mosaic id.
	 * @return The mosaic levy associated with the mosaic.
	 */
	MosaicLevy findById(final MosaicId id);
}
