package org.nem.core.model.mosaic;

import org.nem.core.model.namespace.NamespaceId;

/**
 * Interface for reading mosaic properties.
 */
public interface MosaicProperties {
	/**
	 * Gets the mosaic's description.
	 *
	 * @return The description.
	 */
	String getDescription();

	/**
	 * Gets the number of decimal places up to which the mosaic instance can be partitioned.
	 *
	 * @return The divisibility.
	 */
	int getDivisibility();

	/**
	 * Gets a value indicating whether or not the quantity is mutable.
	 *
	 * @return true if the quantity is mutable, false otherwise.
	 */
	boolean isQuantityMutable();

	/**
	 * Gets the mosaic's name.
	 *
	 * @return The name.
	 */
	String getName();

	/**
	 * Gets the underlying namespace id.
	 *
	 * @return The namespace id.
	 */
	NamespaceId getNamespaceId();

	/**
	 * Gets a value indicating whether or not the the mosaic can be transferred between accounts different from the creator.
	 *
	 * @return true if it can be transferred, false otherwise.
	 */
	boolean isTransferable();
}
