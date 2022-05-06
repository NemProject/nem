package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.state.ReadOnlyAccountImportance;

import java.util.*;
import java.util.stream.*;

/**
 * A filter that filters unconfirmed transactions depending on sender and fill level of the unconfirmed transaction cache. The class limits
 * the number of transaction in the cache to MAX_CACHE_SIZE. For MAX_CACHE_SIZE = 1000 the max allowed transactions graph looks like
 * http://www.wolframalpha.com/input/?i=plot+x+*+1000+*+exp%28-y%2F300%29+*+%281000+-+y%29%2F10%3D1%2C+x%3D0..0.01
 */
public class TransactionSpamFilter {
	private final ReadOnlyNisCache nisCache;
	private final UnconfirmedTransactionsCache transactions;
	private final int maxTransactionsPerBlock;

	/**
	 * Creates a transaction spam filter.
	 *
	 * @param nisCache The (read only) NIS cache.
	 * @param transactions The unconfirmed transactions cache.
	 * @param maxTransactionsPerBlock The maximum number of transactions per block.
	 */
	public TransactionSpamFilter(final ReadOnlyNisCache nisCache, final UnconfirmedTransactionsCache transactions,
			final int maxTransactionsPerBlock) {
		this.nisCache = nisCache;
		this.transactions = transactions;
		this.maxTransactionsPerBlock = maxTransactionsPerBlock;
	}

	/**
	 * Filters out all transactions that are considered spam.
	 *
	 * @param transactions The transactions.
	 * @return The non-spam transactions.
	 */
	public Collection<Transaction> filter(final Collection<Transaction> transactions) {
		final List<Transaction> filteredTransactions = new ArrayList<>();
		transactions.stream().filter(t -> !this.transactions.contains(t) && this.isPermissible(t, filteredTransactions))
				.forEach(filteredTransactions::add);
		return filteredTransactions;
	}

	private boolean isPermissible(final Transaction transaction, final List<Transaction> filteredTransactions) {
		final boolean isMultisig = TransactionTypes.MULTISIG == transaction.getType();
		Collection<Transaction> relevantTransactions = Stream.concat(this.transactions.stream(), filteredTransactions.stream())
				.filter(t -> isMultisig ? TransactionTypes.MULTISIG == t.getType() : TransactionTypes.MULTISIG != t.getType())
				.collect(Collectors.toList());

		if (this.maxTransactionsPerBlock > relevantTransactions.size()) {
			return true;
		}

		final Account debtor = transaction.getDebtor();
		final ReadOnlyAccountImportance importanceInfo = this.nisCache.getAccountStateCache().findStateByAddress(debtor.getAddress())
				.getImportanceInfo();
		final long count = relevantTransactions.stream().filter(t -> t.getDebtor().getAddress().equals(debtor.getAddress())).count();
		final BlockHeight importanceHeight = this.nisCache.getPoxFacade().getLastRecalculationHeight();
		final double importance = importanceHeight.equals(importanceInfo.getHeight())
				? importanceInfo.getImportance(importanceHeight)
				: 0.0;
		final double effectiveImportance = importance + Math.min(0.01, transaction.getFee().getNumMicroNem() / 1000000000.0);
		final int maxAllowedTransactions = this.getMaxAllowedTransactions(effectiveImportance, relevantTransactions.size());
		return count < maxAllowedTransactions;
	}

	private int getMaxAllowedTransactions(final double importance, final int effectiveCacheSize) {
		final int maxCacheSize = 10 * this.maxTransactionsPerBlock;
		return (int) (importance * Math.exp(-3.0 * effectiveCacheSize / maxCacheSize) * 100 * (maxCacheSize - effectiveCacheSize));
	}
}
