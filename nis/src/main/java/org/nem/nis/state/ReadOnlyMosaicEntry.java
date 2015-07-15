package org.nem.nis.state;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.Quantity;

/**
 * A read-only mosaic entry.
 */
public interface ReadOnlyMosaicEntry {

	/**
	 * Gets the mosaic.
	 *
	 * @return The mosaic.
	 */
	Mosaic getMosaic();

	/**
	 * Get the overall supply of the mosaic.
	 *
	 * @return The supply.
	 */
	Quantity getSupply();
}
