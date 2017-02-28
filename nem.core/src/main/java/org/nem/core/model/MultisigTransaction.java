package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A multisig transaction.
 */
public class MultisigTransaction extends Transaction implements SerializableEntity {
	private final Transaction otherTransaction;
	private final Hash otherTransactionHash;
	private final SortedSet<MultisigSignatureTransaction> signatureTransactions = new TreeSet<>(new MultisigSignatureTransactionComparator());

	/**
	 * Creates a multisig transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param otherTransaction The other (enclosed) transaction.
	 */
	public MultisigTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Transaction otherTransaction) {
		super(TransactionTypes.MULTISIG, 1, timeStamp, sender);
		this.otherTransaction = otherTransaction;
		this.otherTransactionHash = HashUtils.calculateHash(otherTransaction.asNonVerifiable());
	}

	/**
	 * Deserializes a multisig transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG, options, deserializer);
		this.otherTransaction = deserializer.readObject("otherTrans", TransactionFactory.NON_VERIFIABLE);
		this.otherTransactionHash = HashUtils.calculateHash(this.otherTransaction.asNonVerifiable());

		final Collection<Transaction> signatures = DeserializationOptions.VERIFIABLE == options
				? deserializer.readObjectArray("signatures", TransactionFactory.VERIFIABLE)
				: new ArrayList<>();

		signatures.forEach(o -> this.addSignature((MultisigSignatureTransaction)o));
	}

	/**
	 * Gets the other transaction.
	 *
	 * @return The other transaction.
	 */
	public Transaction getOtherTransaction() {
		return this.otherTransaction;
	}

	/**
	 * Gets the hash of the other transaction.
	 *
	 * @return The hash of the other transaction.
	 */
	public Hash getOtherTransactionHash() {
		return this.otherTransactionHash;
	}

	/**
	 * Adds a signature to this transaction.
	 *
	 * @param transaction The multisig signature transaction.
	 */
	public void addSignature(final MultisigSignatureTransaction transaction) {
		if (!this.getOtherTransactionHash().equals(transaction.getOtherTransactionHash())) {
			throw new IllegalArgumentException("trying to add a signature for another transaction to a multisig transaction");
		}

		if (!this.otherTransaction.getSigner().equals(transaction.getDebtor())) {
			throw new IllegalArgumentException("trying to add a signature with an unexpected debtor");
		}

		// if the original cosigner is attempting to add an (explicit) signature, ignore it
		// in order to be consistent with how multiple (explicit) signatures from other cosigners
		// are handled (the first one is used and all others are ignored)
		if (this.getSigner().equals(transaction.getSigner())) {
			return;
		}

		this.signatureTransactions.add(transaction);
	}

	/**
	 * Gets list of signature transactions.
	 *
	 * @return The list of signature transactions.
	 */
	public Set<MultisigSignatureTransaction> getCosignerSignatures() {
		return Collections.unmodifiableSet(this.signatureTransactions);
	}

	/**
	 * Gets all signers.
	 *
	 * @return All signers.
	 */
	public List<Account> getSigners() {
		return Collections.unmodifiableList(this.signatureTransactions.stream().map(VerifiableEntity::getSigner).collect(Collectors.toList()));
	}

	@Override
	protected void transfer(final TransactionObserver observer, final TransactionExecutionState state) {
		this.signatureTransactions.stream().forEach(t -> t.transfer(observer, state));
		this.otherTransaction.transfer(observer, state);
		super.transfer(observer, state);
	}

	@Override
	public Account getDebtor() {
		// the multisig account should pay the fee
		return this.otherTransaction.getSigner();
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return this.getChildTransactions().stream()
				.flatMap(t -> t.getAccounts().stream())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<Transaction> getChildTransactions() {
		// we want validators to run on both inner transaction and all signatures
		final List<Transaction> result = new ArrayList<>(this.getCosignerSignatures());
		result.add(this.otherTransaction);
		return result;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		// this shouldn't be called since the other overload is implemented
	}

	@Override
	protected void serializeImpl(final Serializer serializer, final boolean includeNonVerifiableData) {
		super.serializeImpl(serializer);
		serializer.writeObject("otherTrans", this.otherTransaction.asNonVerifiable());

		if (includeNonVerifiableData) {
			serializer.writeObjectArray("signatures", this.signatureTransactions);
		}
	}

	// WARNING: do not alter the following method. Changing it might have
	// disastrous effect on verification of multisig signature transactions.
	@Override
	public boolean verify() {
		return super.verify() && this.signatureTransactions.stream().allMatch(VerifiableEntity::verify);
	}
}