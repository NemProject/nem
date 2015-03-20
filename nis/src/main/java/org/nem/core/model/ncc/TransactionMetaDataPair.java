package org.nem.core.model.ncc;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Pair containing a Transaction and a TransactionMetaData
 */
public class TransactionMetaDataPair implements SerializableEntity {
	private final Transaction transaction;
	private final TransactionMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param transaction The transaction.
	 * @param metaData The meta data.
	 */
	public TransactionMetaDataPair(final Transaction transaction, final TransactionMetaData metaData) {
		this.transaction = transaction;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public TransactionMetaDataPair(final Deserializer deserializer) {
		this(
				deserializer.readObject("transaction", TransactionFactory.VERIFIABLE),
				deserializer.readObject("meta", TransactionMetaData::new));
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("transaction", this.transaction);
		serializer.writeObject("meta", this.metaData);
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
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public TransactionMetaData getMetaData() {
		return this.metaData;
	}
}
