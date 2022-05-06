package org.nem.nis.state;

import org.nem.core.model.primitive.*;

/**
 * Container for vested balances. <br>
 * Methods of this interface assume that they are called in paired order.
 */
public interface WeightedBalances extends ReadOnlyWeightedBalances {

	/**
	 * Creates a deep copy of this weighted balances instance.
	 *
	 * @return A copy of this weighted balances instance.
	 */
	WeightedBalances copy();

	/**
	 * Adds fully vested amount at height.
	 *
	 * @param height The height.
	 * @param amount The amount to vest.
	 */
	void addFullyVested(final BlockHeight height, final Amount amount);

	/**
	 * Adds receive operation of amount at height.
	 *
	 * @param height The height.
	 * @param amount The amount received.
	 */
	void addReceive(final BlockHeight height, final Amount amount);

	/**
	 * Undoes receive operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	void undoReceive(final BlockHeight height, final Amount amount);

	/**
	 * Adds send operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount sent.
	 */
	void addSend(final BlockHeight height, final Amount amount);

	/**
	 * Undoes send operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	void undoSend(final BlockHeight height, final Amount amount);

	/**
	 * Converts the unvested balance to a fully vested balance. This is only possible at height one and if the balances contain exactly one
	 * entry.
	 */
	void convertToFullyVested();

	/**
	 * Undoes all changes to weighted balances after the specified block height. In other words, reverts the weighted balances to the
	 * specified block height.
	 *
	 * @param height The block height.
	 */
	void undoChain(final BlockHeight height);

	/**
	 * Removes all weighted balances that have a height less than minHeight.
	 *
	 * @param minHeight The minimum height of balances to keep.
	 */
	void prune(final BlockHeight minHeight);
}
