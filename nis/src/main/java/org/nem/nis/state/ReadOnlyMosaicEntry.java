package org.nem.nis.state;

import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.primitive.Supply;

/**
 * A read-only mosaic entry.
 */
public interface ReadOnlyMosaicEntry {

	/**
	 * Gets the mosaic definition.
	 *
	 * @return The mosaic definition.
	 */
	MosaicDefinition getMosaicDefinition();

	/**
	 * Get the overall supply of the mosaic.
	 *
	 * @return The supply.
	 */
	Supply getSupply();

	/**
	 * Gets the mosaic balances.
	 *
	 * @return The balances.
	 */
	ReadOnlyMosaicBalances getBalances();
}
