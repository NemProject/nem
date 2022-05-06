package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.model.primitive.HashShortId;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Default implementation of unconfirmed transaction filtering.
 */
public class DefaultUnconfirmedTransactionsFilter implements UnconfirmedTransactionsFilter {
	private final UnconfirmedTransactionsCache transactions;
	private final BiPredicate<Address, Transaction> matchesPredicate;

	/**
	 * Creates a new filter.
	 *
	 * @param transactions The transactions to filter.
	 */
	public DefaultUnconfirmedTransactionsFilter(final UnconfirmedTransactionsCache transactions,
			final BiPredicate<Address, Transaction> matchesPredicate) {
		this.transactions = transactions;
		this.matchesPredicate = matchesPredicate;
	}

	@Override
	public Collection<Transaction> getAll() {
		final List<Transaction> transactions = this.transactions.stream().collect(Collectors.toList());
		return this.sortTransactions(transactions);
	}

	@Override
	public Collection<Transaction> getUnknownTransactions(final Collection<HashShortId> knownHashShortIds) {
		// probably faster to use hash map than collection
		final HashMap<HashShortId, Transaction> unknownHashShortIds = new HashMap<>(this.transactions.size());

		// note: we do not strip down any signatures from multisig transactions.
		// The transaction cache of the remote node will handle duplicates.
		this.transactions.stream().forEach(t -> unknownHashShortIds.put(new HashShortId(HashUtils.calculateHash(t).getShortId()), t));
		this.transactions.stream().flatMap(TransactionExtensions::getChildSignatures)
				.forEach(t -> unknownHashShortIds.put(new HashShortId(HashUtils.calculateHash(t).getShortId()), t));
		knownHashShortIds.stream().forEach(unknownHashShortIds::remove);
		return unknownHashShortIds.values().stream().collect(Collectors.toList());
	}

	@Override
	public Collection<Transaction> getMostRecentTransactionsForAccount(final Address address, final int maxTransactions) {
		return this.transactions.stream().filter(tx -> tx.getType() != TransactionTypes.MULTISIG_SIGNATURE)
				.filter(tx -> this.matchesPredicate.test(address, tx)).sorted((t1, t2) -> -t1.getTimeStamp().compareTo(t2.getTimeStamp()))
				.limit(maxTransactions).collect(Collectors.toList());
	}

	@Override
	public Collection<Transaction> getTransactionsBefore(final TimeInstant time) {
		final List<Transaction> transactions = this.transactions.stream().filter(tx -> tx.getTimeStamp().compareTo(time) < 0)
				// filter out signatures because we don't want them to be directly inside a block
				.filter(tx -> tx.getType() != TransactionTypes.MULTISIG_SIGNATURE).collect(Collectors.toList());

		return this.sortTransactions(transactions);
	}

	private List<Transaction> sortTransactions(final List<Transaction> transactions) {
		Collections.sort(transactions, (lhs, rhs) -> -1 * lhs.compareTo(rhs));
		return transactions;
	}
}
