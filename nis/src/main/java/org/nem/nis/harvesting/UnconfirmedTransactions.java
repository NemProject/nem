package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A collection of unconfirmed transactions.
 */
public interface UnconfirmedTransactions extends UnconfirmedTransactionsFilter {

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	int size();

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

	/**
	 * Removes the specified transaction from the list of unconfirmed transactions.
	 *
	 * @param transaction The transaction to remove.
	 * @return true if the transaction was found and removed; false if the transaction was not found.
	 */
	boolean remove(final Transaction transaction);

	/**
	 * Removes all transactions in the specified block.
	 *
	 * @param block The block.
	 */
	void removeAll(final Block block);

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	void dropExpiredTransactions(final TimeInstant time);
}
