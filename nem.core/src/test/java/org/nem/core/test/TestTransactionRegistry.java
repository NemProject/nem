package org.nem.core.test;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.*;

/**
 * A transaction registry used by test code.
 */
public class TestTransactionRegistry {

	/**
	 * A registry entry.
	 */
	public static class Entry<TModel extends Transaction> {

		/**
		 * The transaction type.
		 */
		public final int type;

		/**
		 * The transaction class
		 */
		public final Class<TModel> modelClass;

		/**
		 * Creates a transaction of the specified type.
		 */
		public final Supplier<TModel> createModel;

		/**
		 * The db table name.
		 */
		public final String tableName;

		private Entry(
				final int type,
				final Class<TModel> modelClass,
				final Supplier<TModel> createModel,
				final String tableName) {
			this.type = type;
			this.modelClass = modelClass;
			this.createModel = createModel;
			this.tableName = tableName;
		}
	}

	private static final List<Entry<?>> ENTRIES = new ArrayList<Entry<?>>() {
		{
			this.add(new Entry<>(
					TransactionTypes.TRANSFER,
					TransferTransaction.class,
					RandomTransactionFactory::createTransfer,
					"Transfers"));

			this.add(new Entry<>(
					TransactionTypes.IMPORTANCE_TRANSFER,
					ImportanceTransferTransaction.class,
					RandomTransactionFactory::createImportanceTransfer,
					"ImportanceTransfers"));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					MultisigAggregateModificationTransaction.class,
					RandomTransactionFactory::createMultisigModification,
					"MultisigSignerModifications"));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG,
					org.nem.core.model.MultisigTransaction.class,
					RandomTransactionFactory::createMultisigTransfer,
					"MultisigTransactions"));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_SIGNATURE,
					org.nem.core.model.MultisigSignatureTransaction.class,
					RandomTransactionFactory::createMultisigSignature,
					null));

			this.add(new Entry<>(
					TransactionTypes.PROVISION_NAMESPACE,
					ProvisionNamespaceTransaction.class,
					RandomTransactionFactory::createProvisionNamespaceTransaction,
					"NamespaceProvisions"));

			this.add(new Entry<>(
					TransactionTypes.MOSAIC_DEFINITION_CREATION,
					MosaicDefinitionCreationTransaction.class,
					RandomTransactionFactory::createMosaicDefinitionCreationTransaction,
					"MosaicCreationTransactions"));

			this.add(new Entry<>(
					TransactionTypes.MOSAIC_SUPPLY_CHANGE,
					MosaicSupplyChangeTransaction.class,
					RandomTransactionFactory::createMosaicSupplyChangeTransaction,
					"MosaicSupplyChanges"));
		}
	};

	/**
	 * Gets all entries.
	 *
	 * @return The entries.
	 */
	@SuppressWarnings("unchecked")
	public static Iterable<Entry<Transaction>> iterate() {
		return () -> ENTRIES.stream().map(e -> (Entry<Transaction>)e).iterator();
	}

	/**
	 * Streams all entries.
	 *
	 * @return The entries.
	 */
	public static Stream<Entry<Transaction>> stream() {
		return StreamSupport.stream(iterate().spliterator(), false);
	}

	/**
	 * Finds an entry given a transaction type.
	 *
	 * @param type The transaction type.
	 * @return The entry.
	 */
	public static Entry<? extends Transaction> findByType(final Integer type) {
		for (final Entry<?> entry : ENTRIES) {
			if (entry.type == type) {
				return entry;
			}
		}

		throw new IllegalArgumentException(String.format("%d type is unknown", type));
	}
}
