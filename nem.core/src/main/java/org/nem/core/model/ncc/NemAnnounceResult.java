package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.ValidationResult;
import org.nem.core.serialization.*;

/**
 * An extended nem request result that is used to convey additional information from an announce operation.
 */
public class NemAnnounceResult extends NemRequestResult {
	private final Hash transactionHash;
	private final Hash innerTransactionHash;

	/**
	 * Creates a NEM announce result from a validation result.
	 *
	 * @param result The validation result.
	 */
	public NemAnnounceResult(final ValidationResult result) {
		this(result, null, null);
	}

	/**
	 * Creates a NEM announce result from a validation result and optional transaction hashes.
	 *
	 * @param result The validation result.
	 * @param transactionHash The transaction hash.
	 * @param innerTransactionHash The inner transaction hash.
	 */
	public NemAnnounceResult(final ValidationResult result, final Hash transactionHash, final Hash innerTransactionHash) {
		super(result);
		this.transactionHash = transactionHash;
		this.innerTransactionHash = innerTransactionHash;
	}

	/**
	 * Deserializes a NEM announce result.
	 *
	 * @param deserializer The deserializer.
	 */
	public NemAnnounceResult(final Deserializer deserializer) {
		super(deserializer);
		this.innerTransactionHash = deserializer.readOptionalObject("innerTransactionHash", Hash::new);
		this.transactionHash = deserializer.readOptionalObject("transactionHash", Hash::new);
	}

	/**
	 * Gets the transaction hash.
	 *
	 * @return The transaction hash.
	 */
	public Hash getTransactionHash() {
		return this.transactionHash;
	}

	/**
	 * Gets the inner transaction hash.
	 *
	 * @return The inner transaction hash.
	 */
	public Hash getInnerTransactionHash() {
		return this.innerTransactionHash;
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		serializer.writeObject("innerTransactionHash", this.innerTransactionHash);
		serializer.writeObject("transactionHash", this.transactionHash);
	}
}
