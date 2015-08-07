package org.nem.core.model.mosaic;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Quantity;

/**
 * Interface for calculating mosaic transfer fees.
 */
public interface MosaicTransferFeeCalculator {

	/**
	 * Calculates the fee for the specified mosaic transfer.
	 *
	 * @param mosaic The mosaic.
	 * @return The fee.
	 */
	Quantity calculateFee(final Mosaic mosaic);

	/**
	 * Gets the fee recipient for the specified mosaic transfer.
	 *
	 * @param mosaic The mosaic.
	 * @return The fee recipient.
	 */
	Account getFeeRecipient(final Mosaic mosaic);
}
