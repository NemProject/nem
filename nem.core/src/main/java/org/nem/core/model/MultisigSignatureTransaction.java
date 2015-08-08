package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A multisig signature transaction.
 */
public class MultisigSignatureTransaction extends Transaction implements SerializableEntity {
	private final Hash otherTransactionHash;
	private final Account multisig;

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param multisig The multisig account.
	 * @param otherTransactionHash The hash of the other transaction.
	 */
	public MultisigSignatureTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account multisig,
			final Hash otherTransactionHash) {
		super(TransactionTypes.MULTISIG_SIGNATURE, 1, timeStamp, sender);
		this.otherTransactionHash = otherTransactionHash;
		this.multisig = multisig;
	}

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param multisig The multisig account.
	 * @param otherTransaction The other transaction.
	 */
	public MultisigSignatureTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Account multisig,
			final Transaction otherTransaction) {
		this(timeStamp, sender, multisig, HashUtils.calculateHash(otherTransaction));
	}

	/**
	 * Deserializes a multisig signature transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigSignatureTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_SIGNATURE, options, deserializer);
		this.otherTransactionHash = deserializer.readObject("otherHash", Hash::new);
		this.multisig = Account.readFrom(deserializer, "otherAccount");
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
	public Account getDebtor() {
		// the multisig account should pay the fee
		return this.multisig;
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		return Collections.emptyList();
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("otherHash", this.getOtherTransactionHash());
		Account.writeTo(serializer, "otherAccount", this.multisig);
	}
}
