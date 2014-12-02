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

	// TODO 20141201 G-J: why do we need it here?
	// TODO 20141201 J-G: where else will the cosigner signature come from?
	private final Signature otherTransactionSignature;

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param otherTransactionHash The hash of the other transaction.
	 * @param otherTransactionSignature The signature of the other transaction.
	 */
	public MultisigSignatureTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Hash otherTransactionHash,
			final Signature otherTransactionSignature) {
		super(TransactionTypes.MULTISIG_SIGNATURE, 1, timeStamp, sender);
		this.otherTransactionHash = otherTransactionHash;
		this.otherTransactionSignature = otherTransactionSignature;
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
		this.otherTransactionSignature = Signature.readFrom(deserializer, "other_sig");
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
	 * Gets the signature of the other transaction.
	 *
	 * @return The signature of the other transaction.
	 */
	public Signature getOtherTransactionSignature() {
		return this.otherTransactionSignature;
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
		Signature.writeTo(serializer, "other_sig", this.getOtherTransactionSignature());
	}
}
