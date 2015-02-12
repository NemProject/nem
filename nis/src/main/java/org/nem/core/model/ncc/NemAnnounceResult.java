package org.nem.core.model.ncc;

import org.nem.core.crypto.Hash;
import org.nem.core.model.ValidationResult;
import org.nem.core.serialization.*;

/**
 * An extended nem request result that is used to convey additional information from an announce operation.
 */
public class NemAnnounceResult extends NemRequestResult {
	private final Hash innerTransactionHash;

	/**
	 * Creates a NEM announce result from a validation result.
	 *
	 * @param result The validation result.
	 */
	public NemAnnounceResult(final ValidationResult result) {
		this(result, null);
	}

	/**
	 * Creates a NEM announce result from a validation result and an inner transaction hash (for multisig transaction).
	 *
	 * @param result The validation result.
	 * @param innerTransactionHash The inner transaction hash.
	 */
	public NemAnnounceResult(final ValidationResult result, final Hash innerTransactionHash) {
		super(result);
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
	}
}
