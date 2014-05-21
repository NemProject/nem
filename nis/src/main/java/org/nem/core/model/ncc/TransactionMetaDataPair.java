package org.nem.core.model.ncc;

import org.nem.core.model.Transaction;
import org.nem.core.model.TransactionFactory;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Connects Transaction with TransactionMetaData
 */
public class TransactionMetaDataPair implements SerializableEntity {
	private Transaction transaction;
	private TransactionMetaData transactionMetaData;

	public TransactionMetaDataPair(final Transaction transaction, final TransactionMetaData transactionMetaData) {
		this.transaction = transaction;
		this.transactionMetaData = transactionMetaData;
	}

	public TransactionMetaDataPair(final Deserializer deserializer) {
		this(
				deserializer.readObject("transaction", TransactionFactory.VERIFIABLE),
				deserializer.readObject("meta", TransactionMetaData.DESERIALIZER)
		);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("transaction", transaction);
		serializer.writeObject("meta", transactionMetaData);
	}

	public Transaction getTransaction() {
		return this.transaction;
	}

	public TransactionMetaData getTransactionMetaData() {
		return this.transactionMetaData;
	}
}
