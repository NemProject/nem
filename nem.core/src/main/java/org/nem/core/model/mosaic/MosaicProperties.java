package org.nem.core.model.mosaic;

import org.nem.core.model.NemProperty;

import java.util.Collection;

/**
 * Interface for reading mosaic properties.
 */
public interface MosaicProperties {
	/**
	 * Gets the number of decimal places up to which the mosaic instance can be partitioned.
	 *
	 * @return The divisibility.
	 */
	int getDivisibility();

	/**
	 * Gets the initial quantity.
	 *
	 * @return The quantity.
	 */
	long getQuantity();

	/**
	 * Gets a value indicating whether or not the quantity is mutable.
	 *
	 * @return true if the quantity is mutable, false otherwise.
	 */
	boolean isQuantityMutable();

	/**
	 * Gets a value indicating whether or not the the mosaic can be transferred between accounts different from the creator.
	 *
	 * @return true if it can be transferred, false otherwise.
	 */
	boolean isTransferable();

	/**
	 * Gets a collection of all property entries in the map.
	 *
	 * @return The collection of entries.
	 */
	Collection<NemProperty> asCollection();
}
