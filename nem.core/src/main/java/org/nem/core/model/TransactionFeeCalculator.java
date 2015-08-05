package org.nem.core.model;

import org.nem.core.model.primitive.*;

/**
 * Interface for calculating and validating transaction fees.
 */
public interface TransactionFeeCalculator {

	/**
	 * Calculates the minimum fee for the specified transaction.
	 *
	 * @param transaction The transaction.
	 * @return The minimum fee.
	 */
	Amount calculateMinimumFee(final Transaction transaction);

	/**
	 * Determines whether the fee for the transaction at the specified block height is valid.
	 *
	 * @param transaction The transaction.
	 * @param blockHeight The block height.
	 * @return true if the transaction fee is valid; false otherwise.
	 */
	boolean isFeeValid(final Transaction transaction, final BlockHeight blockHeight);
}