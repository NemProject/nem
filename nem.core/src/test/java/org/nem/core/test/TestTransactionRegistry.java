package org.nem.core.test;

import org.nem.core.model.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

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
		 * Gets a value indicating whether or not this transaction is embeddable in a multisig transaction.
		 */
		public final boolean isEmbeddableInMultisig;

		private Entry(
				final int type,
				final Class<TModel> modelClass,
				final Supplier<TModel> createModel,
				final boolean isEmbeddableInMultisig) {
			this.type = type;
			this.modelClass = modelClass;
			this.createModel = createModel;
			this.isEmbeddableInMultisig = isEmbeddableInMultisig;
		}
	}

	private static final List<Entry<?>> ENTRIES = new ArrayList<Entry<?>>() {
		{
			this.add(new Entry<>(
					TransactionTypes.TRANSFER,
					TransferTransaction.class,
					RandomTransactionFactory::createTransfer,
					true));

			this.add(new Entry<>(
					TransactionTypes.IMPORTANCE_TRANSFER,
					ImportanceTransferTransaction.class,
					RandomTransactionFactory::createImportanceTransfer,
					true));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					MultisigAggregateModificationTransaction.class,
					RandomTransactionFactory::createMultisigModification,
					true));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG,
					org.nem.core.model.MultisigTransaction.class,
					RandomTransactionFactory::createMultisigTransfer,
					false));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_SIGNATURE,
					org.nem.core.model.MultisigSignatureTransaction.class,
					RandomTransactionFactory::createMultisigSignature,
					false));

			this.add(new Entry<>(
					TransactionTypes.PROVISION_NAMESPACE,
					ProvisionNamespaceTransaction.class,
					RandomTransactionFactory::createProvisionNamespaceTransaction,
					true));

			this.add(new Entry<>(
					TransactionTypes.MOSAIC_CREATION,
					MosaicCreationTransaction.class,
					RandomTransactionFactory::createMosaicCreationTransaction,
					true));
		}
	};

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

	/**
	 * Gets all types as parameterized parameters.
	 * TODO 20150702 J-J: this should use real types!
	 *
	 * @return The parameters
	 */
	public static Collection<Object[]> getTypeParameters() {
		return ENTRIES.stream()
				.map(e -> new Object[] { e.type })
				.collect(Collectors.toList());
	}

	/**
	 * Gets all types as parameterized parameters.
	 * TODO 20150702 J-J: this should use real types!
	 *
	 * @return The parameters
	 */
	public static Collection<Object[]> getMultisigEmbeddableTypeParameters() {
		return ENTRIES.stream()
				.filter(e -> e.isEmbeddableInMultisig)
				.map(e -> new Object[] { e.type })
				.collect(Collectors.toList());
	}
}
