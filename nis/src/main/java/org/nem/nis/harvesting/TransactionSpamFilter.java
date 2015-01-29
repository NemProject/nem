package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.state.ReadOnlyAccountImportance;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * A filter that filters unconfirmed transactions depending on sender and fill level of the unconfirmed transaction cache.
 */
public class TransactionSpamFilter {
	private static final int MAX_CACHE_SIZE = 1000;
	private final ReadOnlyNisCache nisCache;
	private final ConcurrentMap<Hash, Transaction> transactions; // use UnconfirmedTransactionsCache in multisig branch

	/**
	 * Creates a transaction spam filter.
	 *
	 * @param nisCache The (read only) NIS cache.
	 */
	public TransactionSpamFilter(final ReadOnlyNisCache nisCache,final ConcurrentMap<Hash, Transaction> transactions) {
		this.nisCache = nisCache;
		this.transactions = transactions;
	}

	public Collection<Transaction> filter(final Collection<Transaction> transactions) {
		final List<Transaction> filteredTransactions = new ArrayList<>();
		transactions.stream().forEach(t -> {
			if (this.isPermissible(t, filteredTransactions.size())) {
				filteredTransactions.add(t);
			}
		});
		return filteredTransactions;
	}

	private boolean isPermissible(final Transaction transaction, final int numApprovedTransactions) {
		if (BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK > this.transactions.size() + numApprovedTransactions) {
			return true;
		}

		final Account debitor = transaction.getSigner(); // change to transaction.getDebitor() in multisig branch
		final ReadOnlyAccountImportance importanceInfo = this.nisCache
				.getAccountStateCache()
				.findStateByAddress(debitor.getAddress())
				.getImportanceInfo();
		if (!this.nisCache.getPoiFacade().getLastPoiRecalculationHeight().equals(importanceInfo.getHeight())) {
			return false;
		}

		final long count = transactions.values().stream()
				.filter(t -> t.getSigner().getAddress().getEncoded().equals(debitor.getAddress().getEncoded()))  // change to t.getDebitor() in multisig branch
				.count();
		final BlockHeight height = this.nisCache.getPoiFacade().getLastPoiRecalculationHeight();
		return count < getMaxAllowedTransactions(importanceInfo.getImportance(height), numApprovedTransactions);
	}

	private int getMaxAllowedTransactions(final double importance, final int numApprovedTransactions) {
		final int cacheSize = this.transactions.size() + numApprovedTransactions;
		final int maxAllowed = (int)(importance * Math.exp(-cacheSize / 300) * MAX_CACHE_SIZE * (MAX_CACHE_SIZE - cacheSize) / 10);
		return maxAllowed;
	}
}
