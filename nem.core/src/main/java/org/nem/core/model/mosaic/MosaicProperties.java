package org.nem.core.model.mosaic;

import org.nem.core.model.NemProperty;
import org.nem.core.model.namespace.NamespaceId;

import java.util.Collection;

/**
 * Interface for reading mosaic properties.
 * TODO 20150702 J-B: i don't see the point of this interface
 * > because you are essentially "implementing" it in Mosaic
 * > It would make more sense if Mosaic had a getProperties()
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

	/**
	 * Gets a collection of all property entries in the map.
	 *
	 * @return The collection of entries.
	 */
	Collection<NemProperty> asCollection();
}
