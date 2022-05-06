package org.nem.nis.state;

import org.nem.core.model.primitive.*;

public interface ReadOnlyWeightedBalances {

	/**
	 * Gets the size of the weighted balances.
	 *
	 * @return The size.
	 */
	int size();

	/**
	 * Gets the vested amount at the specified height.
	 *
	 * @param height The height.
	 * @return The vested amount.
	 */
	Amount getVested(BlockHeight height);

	/**
	 * Gets the unvested amount at the specified height.
	 *
	 * @param height The height.
	 * @return The unvested amount.
	 */
	Amount getUnvested(BlockHeight height);
}
