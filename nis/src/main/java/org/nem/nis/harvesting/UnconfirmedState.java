package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;

import java.util.Collection;

/**
 * An store of unconfirmed NIS state.
 */
public interface UnconfirmedState {

	/**
	 * Gets the unconfirmed balance for the specified account.
	 *
	 * @param account The account.
	 * @return The unconfirmed balance.
	 */
	Amount getUnconfirmedBalance(final Account account);

	/**
	 * Gets the unconfirmed mosaic balance for the specified account and mosaic id.
	 *
	 * @param account The account.
	 * @param mosaicId The mosaic id.
	 * @return The unconfirmed mosaic balance.
	 */
	Quantity getUnconfirmedMosaicBalance(final Account account, final MosaicId mosaicId);

	/**
	 * Adds new unconfirmed transactions.
	 *
	 * @param transactions The collection of transactions.
	 * @return SUCCESS if all transactions were added successfully, NEUTRAL or FAILURE otherwise.
	 */
	ValidationResult addNewBatch(final Collection<Transaction> transactions);

	/**
	 * Adds a new unconfirmed transaction.
	 *
	 * @param transaction The transaction.
	 * @return The result of transaction validation.
	 */
	ValidationResult addNew(final Transaction transaction);

	/**
	 * Adds an unconfirmed transaction that has been added previously (so some validation checks can be skipped).
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	ValidationResult addExisting(final Transaction transaction);
}
