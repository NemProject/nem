package org.nem.core.test;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.*;

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

		private Entry(
				final int type,
				final Class<TModel> modelClass,
				final Supplier<TModel> createModel) {
			this.type = type;
			this.modelClass = modelClass;
			this.createModel = createModel;
		}
	}

	private static final List<Entry<?>> ENTRIES = new ArrayList<Entry<?>>() {
		{
			this.add(new Entry<>(
					TransactionTypes.TRANSFER,
					TransferTransaction.class,
					RandomTransactionFactory::createTransfer));

			this.add(new Entry<>(
					TransactionTypes.IMPORTANCE_TRANSFER,
					ImportanceTransferTransaction.class,
					RandomTransactionFactory::createImportanceTransfer));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					MultisigAggregateModificationTransaction.class,
					RandomTransactionFactory::createMultisigModification));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG,
					org.nem.core.model.MultisigTransaction.class,
					RandomTransactionFactory::createMultisigTransfer));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_SIGNATURE,
					org.nem.core.model.MultisigSignatureTransaction.class,
					RandomTransactionFactory::createMultisigSignature));

			this.add(new Entry<>(
					TransactionTypes.PROVISION_NAMESPACE,
					ProvisionNamespaceTransaction.class,
					RandomTransactionFactory::createProvisionNamespaceTransaction));

			this.add(new Entry<>(
					TransactionTypes.MOSAIC_CREATION,
					MosaicCreationTransaction.class,
					RandomTransactionFactory::createMosaicCreationTransaction));

			this.add(new Entry<>(
					TransactionTypes.SMART_TILE_SUPPLY_CHANGE,
					SmartTileSupplyChangeTransaction.class,
					RandomTransactionFactory::createSmartTileSupplyChangeTransaction));
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
