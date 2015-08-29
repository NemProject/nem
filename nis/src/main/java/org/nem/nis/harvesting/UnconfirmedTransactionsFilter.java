package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.HashShortId;
import org.nem.core.time.TimeInstant;

import java.util.Collection;

/**
 * An interface for filtering unconfirmed transactions.
 */
public interface UnconfirmedTransactionsFilter {

	/**
	 * Gets all transactions.
	 *
	 * @return All transaction from this unconfirmed transactions.
	 */
	Collection<Transaction> getAll();

	/**
	 * Gets the transactions for which the hash short id is not in the given collection.
	 *
	 * @param knownHashShortIds The collection of known hashes.
	 * @return The unknown transactions.
	 */
	Collection<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds);

	/**
	 * Gets the most recent transactions of an account up to a given limit.
	 *
	 * @param address The address of an account.
	 * @param maxTransactions The maximum number of transactions.
	 * @return The most recent transactions from this unconfirmed transactions.
	 */
	Collection<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxTransactions);

	/**
	 * Gets all transactions before the specified time. Returned list is sorted.
	 *
	 * @param time The specified time.
	 * @return The sorted list of all transactions before the specified time.
	 */
	Collection<Transaction> getTransactionsBefore(final TimeInstant time);
}
