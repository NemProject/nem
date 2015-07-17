package org.nem.core.model;

import org.nem.core.serialization.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Factory class that can deserialize all known transactions.
 */
public class TransactionFactory {

	/**
	 * An object deserializer that wraps this factory and supports deserializing verifiable (signed) transactions.
	 */
	public static final ObjectDeserializer<Transaction> VERIFIABLE =
			deserializer -> deserialize(VerifiableEntity.DeserializationOptions.VERIFIABLE, deserializer);
	/**
	 * An object deserializer that wraps this factory and supports deserializing non-verifiable (unsigned) transactions.
	 */
	public static final ObjectDeserializer<Transaction> NON_VERIFIABLE =
			deserializer -> deserialize(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);

	private static final Map<Integer, BiFunction<VerifiableEntity.DeserializationOptions, Deserializer, Transaction>> typeToConstructorMap =
			new HashMap<Integer, BiFunction<VerifiableEntity.DeserializationOptions, Deserializer, Transaction>>() {
				{
					this.put(TransactionTypes.TRANSFER, TransferTransaction::new);
					this.put(TransactionTypes.IMPORTANCE_TRANSFER, ImportanceTransferTransaction::new);
					this.put(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION, MultisigAggregateModificationTransaction::new);
					this.put(TransactionTypes.MULTISIG, MultisigTransaction::new);
					this.put(TransactionTypes.MULTISIG_SIGNATURE, MultisigSignatureTransaction::new);
					//this.put(TransactionTypes.PROVISION_NAMESPACE, ProvisionNamespaceTransaction::new);
					//this.put(TransactionTypes.MOSAIC_CREATION, MosaicCreationTransaction::new);
					//this.put(TransactionTypes.SMART_TILE_SUPPLY_CHANGE, SmartTileSupplyChangeTransaction::new);
				}
			};

	/**
	 * Gets the number of supported transaction types.
	 *
	 * @return The number of supported transaction types.
	 */
	public static int size() {
		return typeToConstructorMap.size();
	}

	/**
	 * Gets a value indicating whether or not the specific transaction type is supported.
	 *
	 * @param type The type.
	 * @return true if the transaction type is supported.
	 */
	public static boolean isSupported(final int type) {
		return typeToConstructorMap.containsKey(type);
	}

	/**
	 * Deserializes a transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 * @return The deserialized transaction.
	 */
	private static Transaction deserialize(final VerifiableEntity.DeserializationOptions options, final Deserializer deserializer) {
		final int type = deserializer.readInt("type");

		final BiFunction<VerifiableEntity.DeserializationOptions, Deserializer, Transaction> constructor = typeToConstructorMap.getOrDefault(type, null);
		if (null == constructor) {
			throw new IllegalArgumentException("Unknown transaction type: " + type);
		}

		return constructor.apply(options, deserializer);
	}
}
