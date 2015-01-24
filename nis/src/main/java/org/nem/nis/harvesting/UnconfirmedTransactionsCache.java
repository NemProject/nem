package org.nem.nis.harvesting;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A cache of all unconfirmed transactions.
 */
public class UnconfirmedTransactionsCache {
	private final Function<Transaction, ValidationResult> validate;
	private final BiPredicate<MultisigSignatureTransaction, MultisigTransaction> isMatch;
	private final Map<Hash, Transaction> transactions = new ConcurrentHashMap<>();
	private final Set<Hash> childTransactions = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * Creates a new cache with no transaction validation.
	 */
	public UnconfirmedTransactionsCache() {
		this(t -> ValidationResult.SUCCESS, (mst, mt) -> false);
	}

	/**
	 * Creates a new cache with transaction validation.
	 *
	 * @param validate The validation function.
	 * @param isMatch A predicate that returns true if a multisig signature transaction matches a multisig transaction.
	 */
	public UnconfirmedTransactionsCache(
			final Function<Transaction, ValidationResult> validate,
			final BiPredicate<MultisigSignatureTransaction, MultisigTransaction> isMatch) {
		this.validate = validate;
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
		return this.transactions.size() + this.childTransactions.size();
	}

	/**
	 * Removes all transactions from this cache.
	 */
	public void clear() {
		this.transactions.clear();
		this.childTransactions.clear();
	}

	/**
	 * Streams all root transactions.
	 *
	 * @return The transaction stream.
	 */
	public Stream<Transaction> stream() {
		return this.transactions.values().stream();
	}

	/**
	 * Streams all root transactions and their children.
	 *
	 * @return The transaction stream.
	 */
	public Stream<Transaction> streamFlat() {
		return Stream.concat(
				this.transactions.values().stream(),
				this.transactions.values().stream().flatMap(t -> t.getChildTransactions().stream()));
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

		final ValidationResult result = this.validate.apply(transaction);
		if (!result.isSuccess()) {
			return result;
		}

		if (TransactionTypes.MULTISIG_SIGNATURE == transaction.getType()) {
			return this.handleMultisigSignature((MultisigSignatureTransaction)transaction, transactionHash);
		}

		this.addTransactionToCache(transaction, transactionHash);
		return ValidationResult.SUCCESS;
	}

	private ValidationResult handleMultisigSignature(
			final MultisigSignatureTransaction signatureTransaction,
			final Hash signatureTransactionHash) {
		final Optional<MultisigTransaction> multisigTransaction = this.transactions.values().stream()
				.filter(t -> TransactionTypes.MULTISIG == t.getType())
				.map(t -> (MultisigTransaction)t)
				.filter(mt -> this.isMatch.test(signatureTransaction, mt))
				.findAny();

		if (!multisigTransaction.isPresent()) {
			return ValidationResult.FAILURE_MULTISIG_NO_MATCHING_MULTISIG;
		}

		this.childTransactions.add(signatureTransactionHash);
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
		if (!this.hasTransactionInCache(transaction, transactionHash)) {
			return false;
		}

		this.removeTransactionFromCache(transaction, transactionHash);
		return true;
	}

	private boolean hasTransactionInCache(final Transaction transaction, final Hash transactionHash) {
		return this.transactions.containsKey(transactionHash) ||
				this.childTransactions.contains(transactionHash) ||
				transaction.getChildTransactions().stream()
						.anyMatch(t -> {
							final Hash key = HashUtils.calculateHash(t);
							return this.childTransactions.contains(key) || this.transactions.containsKey(key);
						});
	}

	private void addTransactionToCache(final Transaction transaction, final Hash transactionHash) {
		for (final Transaction childTransaction : transaction.getChildTransactions()) {
			this.childTransactions.add(HashUtils.calculateHash(childTransaction));
		}

		this.transactions.put(transactionHash, transaction);
	}

	private void removeTransactionFromCache(final Transaction transaction, final Hash transactionHash) {
		for (final Transaction childTransaction : transaction.getChildTransactions()) {
			this.childTransactions.remove(HashUtils.calculateHash(childTransaction));
		}

		this.transactions.remove(transactionHash);
	}
}
