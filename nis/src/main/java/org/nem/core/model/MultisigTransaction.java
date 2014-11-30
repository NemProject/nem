package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.Amount;
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
	private final List<MultisigSignatureTransaction> signatureTransactions = new ArrayList<>();

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
		this.otherTransaction = deserializer.readObject("other_trans", TransactionFactory.NON_VERIFIABLE);
		this.otherTransactionHash = HashUtils.calculateHash(otherTransaction.asNonVerifiable());
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
	 * TODO this should get called by unconfirmed transactions
	 *
	 * @param transaction The multisig signature transaction.
	 */
	public void addSignature(final MultisigSignatureTransaction transaction) {
		// TODO: add validation!
		//if (this.otherTransaction.verify(transaction.getSigner()) {
		//	throw new IllegalArgumentException("signature is not valid");
		//}

		if (!this.getOtherTransactionHash().equals(transaction.getOtherTransactionHash())) {
			throw new IllegalArgumentException("trying to add a signature for another transaction to a multisig transaction");
		}

		this.signatureTransactions.add(transaction);
	}

	/**
	 * Gets all signers.
	 * TODO this should get called by unconfirmed transactions
	 *
	 * @return All signers.
	 */
	public List<Account> getSigners() {
		final List<Account> signers = new ArrayList<>();
		signers.add(this.getSigner());
		signers.addAll(this.signatureTransactions.stream().map(t -> t.getSigner()).collect(Collectors.toList()));
		return signers;
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
		this.otherTransaction.transfer(observer);
	}

	@Override
	protected Amount getMinimumFee() {
		return Amount.fromNem(100L).add(this.otherTransaction.getMinimumFee());
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("other_trans", this.getOtherTransaction().asNonVerifiable());
		// TODO: need to add some other fields here (not complete)
	}
}