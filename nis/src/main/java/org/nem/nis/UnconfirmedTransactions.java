package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.Predicate;
import org.nem.nis.dbmodel.*;
import sun.management.Sensor;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * A collection of unconfirmed transactions.
 */
public class UnconfirmedTransactions {

	private final ConcurrentMap<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Account, Amount> unconfirmedBalances = new ConcurrentHashMap<>();

	private final AccountLookup accountLookup;

	UnconfirmedTransactions(AccountLookup accountLookup) {
		this.accountLookup = accountLookup;
	}

	/**
	 * Gets the number of unconfirmed transactions.
	 *
	 * @return The number of unconfirmed transactions.
	 */
	public int size() {
		return this.transactions.size();
	}

	/**
	 * Adds an unconfirmed transaction unconditionally.
	 *
	 * @param transaction The transaction.
	 * @return true if the transaction was added.
	 */
	boolean add(final Transaction transaction) {
		return this.add(transaction, hash -> false);
	}

	/**
	 * Adds an unconfirmed transaction if and only if the predicate evaluates to false.
	 *
	 * @param transaction The transaction.
	 * @param exists Predicate that determines the existence of the transaction given its hash.
	 * @return true if the transaction was added.
	 */
	boolean add(final Transaction transaction, final Predicate<Hash> exists) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (exists.evaluate(transactionHash)) {
			return false;
		}

		if (! transaction.simulateExecute(
				new NemTransferSimulate() {
					@Override
					public boolean sub(Account sender, Amount amount) {
						addToCache(sender);
						if (unconfirmedBalances.get(sender).compareTo(amount) < 0) {
							return false;
						}
						Amount newBalance = unconfirmedBalances.get(sender).subtract(amount);
						unconfirmedBalances.replace(sender, newBalance);
						return true;
					}

					@Override
					public void add(Account recipient, Amount amount) {
						addToCache(recipient);
						Amount newBalance = unconfirmedBalances.get(recipient).add(amount);
						unconfirmedBalances.replace(recipient, newBalance);
					}
				}
		)) {
			return false;
		}

		final Transaction previousTransaction = this.transactions.putIfAbsent(transactionHash, transaction);
		return null == previousTransaction;
	}

	private void addToCache(Account account) {
		// it's ok to put reference here, thanks to Account being non-mutable
		this.unconfirmedBalances.putIfAbsent(account, account.getBalance());
	}

	/**
	 * Removes all transactions in the specified block.
	 *
	 * @param block The block.
	 */
	void removeAll(final Block block) {
		for (final Transaction transaction : block.getTransactions()) {
			final Hash transactionHash = HashUtils.calculateHash(transaction);
			this.transactions.remove(transactionHash);
		}
	}

	/**
	 * Gets all transactions before the specified time.
	 *
	 * @param time The specified time.
	 * @return All transactions before the specified time.
	 */
	public List<Transaction> getTransactionsBefore(final TimeInstant time) {
		final List<Transaction> transactions =  this.transactions.values().stream()
				.filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
				.collect(Collectors.toList());

		Collections.sort(transactions, (lhs, rhs) -> {
			// should we just use Transaction.compare (it weights things other than fees more heavily) ?
			// maybe we should change Transaction.compare? also it
			// TODO: should fee or time be more important inside Transaction.compare
			int result = -lhs.getFee().compareTo(rhs.getFee());
			if (result == 0) {
				result = lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
			}
			return result;
		});

		return transactions;
	}
}
