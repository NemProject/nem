package org.nem.core.model.ncc;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Pair containing an unconfirmed transaction and an unconfirmed transaction meta data.
 */
public class UnconfirmedTransactionMetaDataPair implements SerializableEntity {

	private final Transaction transaction;
	private final UnconfirmedTransactionMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param transaction The unconfirmed transaction.
	 * @param metaData The meta data.
	 */
	public UnconfirmedTransactionMetaDataPair(final Transaction transaction, final UnconfirmedTransactionMetaData metaData) {
		this.transaction = transaction;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public UnconfirmedTransactionMetaDataPair(final Deserializer deserializer) {
		this(
				deserializer.readObject("transaction", TransactionFactory.VERIFIABLE),
				deserializer.readObject("meta", UnconfirmedTransactionMetaData::new));
	}

	/**
	 * Gets the unconfirmed transaction.
	 *
	 * @return The unconfirmed transaction.
	 */
	public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public UnconfirmedTransactionMetaData getMetaData() {
		return this.metaData;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("transaction", this.transaction);
		serializer.writeObject("meta", this.metaData);
	}
}
