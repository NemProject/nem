package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A collection of unconfirmed transactions.
 */
public interface UnconfirmedTransactions extends UnconfirmedState {

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	int size();

	/**
	 * Removes the specified transaction from the list of unconfirmed transactions.
	 *
	 * @param transaction The transaction to remove.
	 * @return true if the transaction was found and removed; false if the transaction was not found.
	 */
	boolean remove(final Transaction transaction);

	/**
	 * Removes all specified transactions.
	 *
	 * @param transactions The transactions.
	 */
	void removeAll(final Collection<Transaction> transactions);

	/**
	 * Drops transactions that have already expired.
	 *
	 * @param time The current time.
	 */
	void dropExpiredTransactions(final TimeInstant time);

	/**
	 * Gets a filter that can be used for querying the unconfirmed transactions.
	 *
	 * @return The filter.
	 */
	UnconfirmedTransactionsFilter asFilter();
}
