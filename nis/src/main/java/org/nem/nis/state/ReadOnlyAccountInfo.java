package org.nem.nis.state;

import org.nem.core.model.primitive.*;

/**
 * Read-only account info.
 */
public interface ReadOnlyAccountInfo {

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	public Amount getBalance();

	/**
	 * Gets number of harvested blocks.
	 *
	 * @return Number of blocks harvested by the account.
	 */
	public BlockAmount getHarvestedBlocks();

	/**
	 * Gets the account's label.
	 *
	 * @return The account's label.
	 */
	public String getLabel();

	/**
	 * Returns the reference count.
	 *
	 * @return The reference count.
	 */
	public ReferenceCount getReferenceCount();

}
