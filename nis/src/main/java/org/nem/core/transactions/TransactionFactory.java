package org.nem.core.transactions;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Factory class that can deserialize all known transactions.
 */
public class TransactionFactory {

	/**
	 * An object deserializer that wraps this factory.
	 */
	public static final ObjectDeserializer<Transaction> VERIFIABLE = new ObjectDeserializer<Transaction>() {
		@Override
		public Transaction deserialize(Deserializer deserializer) {
			return TransactionFactory.deserialize(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
		}
	};

	public static final ObjectDeserializer<Transaction> NON_VERIFIABLE = new ObjectDeserializer<Transaction>() {
		@Override
		public Transaction deserialize(Deserializer deserializer) {
			return TransactionFactory.deserialize(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
		}
	};

	/**
	 * Deserializes a transaction.
	 *
	 * @param options      The deserialization options.
	 * @param deserializer The deserializer.
	 *
	 * @return The deserialized transaction.
	 */
	private static Transaction deserialize(final VerifiableEntity.DeserializationOptions options, final Deserializer deserializer) {
		int type = deserializer.readInt("type");

		switch (type) {
			case TransactionTypes.TRANSFER:
				return new TransferTransaction(options, deserializer);
		}

		throw new InvalidParameterException("Unknown transaction type: " + type);
	}
}
