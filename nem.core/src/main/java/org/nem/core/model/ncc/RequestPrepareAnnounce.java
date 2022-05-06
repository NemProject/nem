package org.nem.core.model.ncc;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Represents a prepare announce request that is used to prepare and announce a transaction.
 */
public class RequestPrepareAnnounce implements SerializableEntity {
	private final Transaction transaction;
	private final PrivateKey privateKey;

	/**
	 * Creates a new request.
	 *
	 * @param transaction The transaction.
	 * @param privateKey The private key.
	 */
	public RequestPrepareAnnounce(final Transaction transaction, final PrivateKey privateKey) {
		this.transaction = transaction;
		this.privateKey = privateKey;
	}

	/**
	 * Deserializes a request.
	 *
	 * @param deserializer The deserializer.
	 */
	public RequestPrepareAnnounce(final Deserializer deserializer) {
		this.transaction = deserializer.readObject("transaction", TransactionFactory.NON_VERIFIABLE);
		this.privateKey = PrivateKey.fromHexString(deserializer.readString("privateKey"));
	}

	/**
	 * Gets the transaction.
	 *
	 * @return The transaction.
	 */
	public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * Gets the private key.
	 *
	 * @return The private key.
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("transaction", this.getTransaction().asNonVerifiable());
		serializer.writeString("privateKey", this.getPrivateKey().toString());
	}
}
