package org.nem.core.model.ncc;

import org.nem.core.model.Transaction;
import org.nem.core.model.TransactionFactory;
import org.nem.core.serialization.*;

/**
 * Pair containing a Transaction and a TransactionMetaData
 */
public class TransactionMetaDataPair implements SerializableEntity {
	public static ObjectDeserializer<TransactionMetaDataPair> DESERIALIZER =
			deserializer -> new TransactionMetaDataPair(deserializer);
	private Transaction transaction;
	private TransactionMetaData metaData;

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
				deserializer.readObject("meta", obj -> new TransactionMetaData(obj)));
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
