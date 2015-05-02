package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.state.ReadOnlyAccountImportance;

import java.util.*;
import java.util.stream.Stream;

/**
 * A filter that filters unconfirmed transactions depending on sender and fill level of the unconfirmed transaction cache.
 * The class limits the number of transaction in the cache to MAX_CACHE_SIZE.
 * For MAX_CACHE_SIZE = 1000 the max allowed transactions graph looks like
 * http://www.wolframalpha.com/input/?i=plot+x+*+1000+*+exp%28-y%2F300%29+*+%281000+-+y%29%2F10%3D1%2C+x%3D0..0.01
 */
public class TransactionSpamFilter {
	private static final double MAX_CACHE_SIZE = 1000.0;
	private final ReadOnlyNisCache nisCache;
	private final UnconfirmedTransactionsCache transactions;

	/**
	 * Creates a transaction spam filter.
	 *
	 * @param nisCache The (read only) NIS cache.
	 * @param transactions The unconfirmed transactions cache.
	 */
	public TransactionSpamFilter(final ReadOnlyNisCache nisCache, final UnconfirmedTransactionsCache transactions) {
		this.nisCache = nisCache;
		this.transactions = transactions;
	}

	/**
	 * Filters out all transactions that are considered spam.
	 *
	 * @param transactions The transactions.
	 * @return The non-spam transactions.
	 */
	public Collection<Transaction> filter(final Collection<Transaction> transactions) {
		final List<Transaction> filteredTransactions = new ArrayList<>();
		transactions.stream()
				.filter(t -> !this.transactions.contains(t) && this.isPermissible(t, filteredTransactions))
				.forEach(filteredTransactions::add);
		return filteredTransactions;
	}

	private boolean isPermissible(final Transaction transaction, final List<Transaction> filteredTransactions) {
		final int numApprovedTransactions = this.flatSize(filteredTransactions);
		if (BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK > this.transactions.flatSize() + numApprovedTransactions) {
			return true;
		}

		final Account debtor = transaction.getDebtor();
		final ReadOnlyAccountImportance importanceInfo = this.nisCache
				.getAccountStateCache()
				.findStateByAddress(debtor.getAddress())
				.getImportanceInfo();
		final long count = Stream.concat(this.transactions.stream(), filteredTransactions.stream())
				.filter(t -> t.getDebtor().getAddress().equals(debtor.getAddress()))
				.count();
		final BlockHeight importanceHeight = this.nisCache.getPoiFacade().getLastPoiRecalculationHeight();
		final double importance = importanceHeight.equals(importanceInfo.getHeight()) ? importanceInfo.getImportance(importanceHeight) : 0.0;
		final double effectiveImportance = importance + Math.min(0.01, transaction.getFee().getNumNem() / 100000.0);

		return count < this.getMaxAllowedTransactions(effectiveImportance, numApprovedTransactions);
	}

	private int getMaxAllowedTransactions(final double importance, final int numApprovedTransactions) {
		final double cacheSize = this.transactions.flatSize() + numApprovedTransactions;
		return (int)(importance * Math.exp(-cacheSize / 300) * MAX_CACHE_SIZE * (MAX_CACHE_SIZE - cacheSize) / 10);
	}

	private int flatSize(final List<Transaction> filteredTransactions) {
		return (int)filteredTransactions.stream().flatMap(TransactionExtensions::streamDefault).count();
	}
}
