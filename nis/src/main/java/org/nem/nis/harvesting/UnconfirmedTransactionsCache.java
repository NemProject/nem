package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.*;

/**
 * A cache of all unconfirmed transactions. <br>
 * Note that this class is not inherently threadsafe, but it is used in a threadsafe way by UnconfirmedTransactions.
 */
public class UnconfirmedTransactionsCache {
	private final BiPredicate<MultisigSignatureTransaction, MultisigTransaction> isMatch;
	private final List<TransactionListEntry> transactions = new ArrayList<>();
	private final Set<Hash> transactionHashes = new HashSet<>();
	private final Set<Hash> childTransactionHashes = new HashSet<>();

	/**
	 * Creates a new cache with no transaction validation.
	 */
	public UnconfirmedTransactionsCache() {
		this((mst, mt) -> false);
	}

	/**
	 * Creates a new cache with transaction validation.
	 *
	 * @param isMatch A predicate that returns true if a multisig signature transaction matches a multisig transaction.
	 */
	public UnconfirmedTransactionsCache(final BiPredicate<MultisigSignatureTransaction, MultisigTransaction> isMatch) {
		this.isMatch = isMatch;
	}

	/**
	 * Gets the number of root transactions.
	 *
	 * @return The number of root transactions.
	 */
	public int size() {
		return this.transactions.size();
	}

	/**
	 * Gets the number of root transactions and their children.
	 *
	 * @return The number of root transactions and their children.
	 */
	public int flatSize() {
		return this.transactions.size() + this.childTransactionHashes.size();
	}

	/**
	 * Removes all transactions from this cache.
	 */
	public void clear() {
		this.transactions.clear();
		this.transactionHashes.clear();
		this.childTransactionHashes.clear();
	}

	/**
	 * Streams all root transactions.
	 *
	 * @return The transaction stream.
	 */
	public Stream<Transaction> stream() {
		return this.transactions.stream().map(e -> e.transaction);
	}

	/**
	 * Streams all root transactions and their children.
	 *
	 * @return The transaction stream.
	 */
	public Stream<Transaction> streamFlat() {
		return this.stream().flatMap(TransactionExtensions::streamDefault);
	}

	/**
	 * Adds a transaction to the cache.
	 *
	 * @param transaction The transaction to add.
	 * @return SUCCESS if the transaction was added.
	 */
	public ValidationResult add(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		if (this.hasTransactionInCache(transaction, transactionHash)) {
			return ValidationResult.NEUTRAL;
		}

		if (TransactionTypes.MULTISIG_SIGNATURE == transaction.getType()) {
			return this.handleMultisigSignature((MultisigSignatureTransaction) transaction, transactionHash);
		}

		this.addTransactionToCache(transaction, transactionHash);
		return ValidationResult.SUCCESS;
	}

	private ValidationResult handleMultisigSignature(final MultisigSignatureTransaction signatureTransaction,
			final Hash signatureTransactionHash) {
		final Optional<MultisigTransaction> multisigTransaction = this.transactions.stream().map(e -> e.transaction)
				.filter(t -> TransactionTypes.MULTISIG == t.getType()).map(t -> (MultisigTransaction) t)
				.filter(mt -> this.isMatch.test(signatureTransaction, mt)).findAny();

		if (!multisigTransaction.isPresent()) {
			return ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
		}

		this.childTransactionHashes.add(signatureTransactionHash);
		multisigTransaction.get().addSignature(signatureTransaction);
		return ValidationResult.SUCCESS;
	}

	/**
	 * Gets a value indicating whether or not this cache contains the transaction.
	 *
	 * @param transaction The transaction to add.
	 * @return true if the transaction is contained.
	 */
	public boolean contains(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);
		return this.hasTransactionInCache(transaction, transactionHash);
	}

	/**
	 * Removes a transaction from the cache.
	 *
	 * @param transaction The transaction to remove.
	 * @return true if the transaction was removed.
	 */
	public boolean remove(final Transaction transaction) {
		final Hash transactionHash = HashUtils.calculateHash(transaction);

		// do not call hasTransactionInCache here because only root transactions can be removed
		if (!this.transactionHashes.contains(transactionHash)) {
			return false;
		}

		this.removeTransactionFromCache(transaction, transactionHash);
		return true;
	}

	private boolean hasTransactionInCache(final Transaction transaction, final Hash transactionHash) {
		return this.transactionHashes.contains(transactionHash) || this.childTransactionHashes.contains(transactionHash)
				|| transaction.getChildTransactions().stream().anyMatch(t -> {
					final Hash key = HashUtils.calculateHash(t);
					return this.childTransactionHashes.contains(key) || this.transactionHashes.contains(key);
				});
	}

	private void addTransactionToCache(final Transaction transaction, final Hash transactionHash) {
		this.childTransactionHashes
				.addAll(transaction.getChildTransactions().stream().map(HashUtils::calculateHash).collect(Collectors.toList()));
		this.transactions.add(new TransactionListEntry(transaction, transactionHash));
		this.transactionHashes.add(transactionHash);
	}

	private void removeTransactionFromCache(final Transaction transaction, final Hash transactionHash) {
		for (final Transaction childTransaction : transaction.getChildTransactions()) {
			this.childTransactionHashes.remove(HashUtils.calculateHash(childTransaction));
		}

		this.transactions.remove(new TransactionListEntry(transaction, transactionHash));
		this.transactionHashes.remove(transactionHash);
	}

	private static class TransactionListEntry {
		public final Transaction transaction;
		public final Hash hash;

		public TransactionListEntry(final Transaction transaction, final Hash hash) {
			this.transaction = transaction;
			this.hash = hash;
		}

		@Override
		public int hashCode() {
			return this.hash.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null || !(obj instanceof TransactionListEntry)) {
				return false;
			}

			final TransactionListEntry rhs = (TransactionListEntry) obj;
			return this.hash.equals(rhs.hash);
		}
	}
}
