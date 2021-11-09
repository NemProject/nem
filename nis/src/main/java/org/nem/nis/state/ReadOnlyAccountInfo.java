package org.nem.nis.state;

import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;

import java.util.Collection;

/**
 * Read-only account info.
 */
public interface ReadOnlyAccountInfo {

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	Amount getBalance();

	/**
	 * Gets number of harvested blocks.
	 *
	 * @return Number of blocks harvested by the account.
	 */
	BlockAmount getHarvestedBlocks();

	/**
	 * Gets the account's label.
	 *
	 * @return The account's label.
	 */
	String getLabel();

	/**
	 * Returns the reference count. <br>
	 * Note that this is readonly because ReferenceCount is immutable.
	 *
	 * @return The reference count.
	 */
	ReferenceCount getReferenceCount();

	/**
	 * Gets the mosaic ids this account is invested in.
	 *
	 * @return The mosaic ids.
	 */
	Collection<MosaicId> getMosaicIds();
}
