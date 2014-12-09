package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * A multisig signature transaction.
 */
public class MultisigSignatureTransaction extends Transaction implements SerializableEntity {
	private final Hash otherTransactionHash;

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param otherTransactionHash The hash of the other transaction.
	 */
	public MultisigSignatureTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Hash otherTransactionHash) {
		super(TransactionTypes.MULTISIG_SIGNATURE, 1, timeStamp, sender);
		this.otherTransactionHash = otherTransactionHash;
	}

	/**
	 * Deserializes a multisig signature transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigSignatureTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_SIGNATURE, options, deserializer);
		this.otherTransactionHash = deserializer.readObject("other_hash", Hash::new);
	}

	/**
	 * Gets the hash of the other transaction.
	 *
	 * @return The hash of the other transaction.
	 */
	public Hash getOtherTransactionHash() {
		return this.otherTransactionHash;
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
	}

	@Override
	protected Amount getMinimumFee() {
		return Amount.ZERO;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("other_hash", this.getOtherTransactionHash());
	}
}
