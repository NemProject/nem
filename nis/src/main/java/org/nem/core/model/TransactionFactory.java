package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Factory class that can deserialize all known transactions.
 */
public class TransactionFactory {

	/**
	 * An object deserializer that wraps this factory.
	 */
	public static final ObjectDeserializer<Transaction> VERIFIABLE =
			deserializer -> deserialize(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);

	public static final ObjectDeserializer<Transaction> NON_VERIFIABLE =
			deserializer -> deserialize(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);

	/**
	 * Deserializes a transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 * @return The deserialized transaction.
	 */
	private static Transaction deserialize(final VerifiableEntity.DeserializationOptions options, final Deserializer deserializer) {
		final int type = deserializer.readInt("type");

		switch (type) {
			case TransactionTypes.TRANSFER:
				return new TransferTransaction(options, deserializer);

			case TransactionTypes.IMPORTANCE_TRANSFER:
				return new ImportanceTransfer(options, deserializer);
		}

		throw new IllegalArgumentException("Unknown transaction type: " + type);
	}
}
