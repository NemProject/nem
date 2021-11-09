package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dao.retrievers.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Contains transaction mapping metadata.
 */
@SuppressWarnings("rawtypes")
public class TransactionRegistry {

	/**
	 * A registry entry.
	 */
	public static class Entry<TDbModel extends AbstractBlockTransfer, TModel extends Transaction> {

		/**
		 * The transaction type.
		 */
		public final int type;

		/**
		 * A function that will return db model transactions given a block
		 */
		public final Function<DbBlock, List<TDbModel>> getFromBlock;

		/**
		 * A function that will return set db model transactions given a block
		 */
		public final BiConsumer<DbBlock, List<TDbModel>> setInBlock;

		/**
		 * A function that will get db model transactions given a multisig transfer.
		 */
		public final Function<DbMultisigTransaction, TDbModel> getFromMultisig;

		/**
		 * A function that will set a db model transactions in a given multisig transfer.
		 */
		public final BiConsumer<DbMultisigTransaction, AbstractBlockTransfer> setInMultisig;

		/**
		 * A function that will get the inner transaction or null if none is available.
		 */
		public final Function<? super TDbModel, ? extends AbstractBlockTransfer> getInnerTransaction;

		/**
		 * A function that will get the number of transactions involved in the given transaction.
		 */
		public final Function<TDbModel, Integer> getTransactionCount;

		/**
		 * A function that will get the recipient (if any) given an abstract block transfer.
		 */
		public final Function<? super TDbModel, DbAccount> getRecipient;

		/**
		 * A function that will get a list of db accounts (if any) given an abstract block transfer.
		 */
		public final Function<? super TDbModel, Collection<DbAccount>> getOtherAccounts;

		/**
		 * A supplier for transaction retrievers.
		 */
		public final Supplier<TransactionRetriever> getTransactionRetriever;

		/**
		 * The db model transaction class.
		 */
		public final Class<TDbModel> dbModelClass;

		/**
		 * The model transaction class.
		 */
		public final Class<TModel> modelClass;

		/**
		 * The name of the db column used to join this transaction with the multisig table.
		 */
		public final String multisigJoinField;

		private final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper;
		private final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper;

		private Entry(final int type, final Function<DbBlock, List<TDbModel>> getFromBlock,
				final BiConsumer<DbBlock, List<TDbModel>> setInBlock, final Function<DbMultisigTransaction, TDbModel> getFromMultisig,
				final BiConsumer<DbMultisigTransaction, AbstractBlockTransfer> setInMultisig,
				final Function<TDbModel, AbstractBlockTransfer> getInnerTransaction, final Function<TDbModel, Integer> getTransactionCount,
				final Function<TDbModel, DbAccount> getRecipient, final Function<TDbModel, Collection<DbAccount>> getOtherAccounts,
				final Supplier<TransactionRetriever> getTransactionRetriever,
				final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper,
				final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper, final Class<TDbModel> dbModelClass,
				final Class<TModel> modelClass, final String multisigJoinField) {
			this.type = type;

			this.getFromBlock = getFromBlock;
			this.setInBlock = setInBlock;

			this.getFromMultisig = getFromMultisig;
			this.setInMultisig = setInMultisig;
			this.getInnerTransaction = getInnerTransaction;
			this.getTransactionCount = getTransactionCount;

			this.getRecipient = getRecipient;
			this.getOtherAccounts = getOtherAccounts;

			this.getTransactionRetriever = getTransactionRetriever;

			this.createModelToDbModelMapper = createModelToDbModelMapper;
			this.createDbModelToModelMapper = createDbModelToModelMapper;
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
			this.multisigJoinField = multisigJoinField;
		}

		/**
		 * Adds model to db model mappers to the mapping repository.
		 *
		 * @param repository The mapping repository
		 */
		public void addModelToDbModelMappers(final MappingRepository repository) {
			repository.addMapping(this.modelClass, this.dbModelClass, this.createModelToDbModelMapper.apply(repository));
		}

		/**
		 * Adds db model to model mappers to the mapping repository.
		 *
		 * @param repository The mapping repository
		 */
		public void addDbModelToModelMappers(final MappingRepository repository) {
			repository.addMapping(this.dbModelClass, this.modelClass, this.createDbModelToModelMapper.apply(repository));
			repository.addMapping(this.dbModelClass, Transaction.class, this.createDbModelToModelMapper.apply(repository));
		}
	}

	@SuppressWarnings("serial")
	private static final List<Entry<?, ?>> ENTRIES = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(TransactionTypes.TRANSFER, DbBlock::getBlockTransferTransactions, DbBlock::setBlockTransferTransactions,
					DbMultisigTransaction::getTransferTransaction,
					(multisig, t) -> multisig.setTransferTransaction((DbTransferTransaction) t), transfer -> null, transfer -> 1,
					DbTransferTransaction::getRecipient, transfer -> new ArrayList<>(), TransferRetriever::new,
					TransferModelToDbModelMapping::new, TransferDbModelToModelMapping::new, DbTransferTransaction.class,
					TransferTransaction.class, "transferTransaction"));

			this.add(new Entry<>(TransactionTypes.IMPORTANCE_TRANSFER, DbBlock::getBlockImportanceTransferTransactions,
					DbBlock::setBlockImportanceTransferTransactions, DbMultisigTransaction::getImportanceTransferTransaction,
					(multisig, t) -> multisig.setImportanceTransferTransaction((DbImportanceTransferTransaction) t), transfer -> null,
					transfer -> 1, DbImportanceTransferTransaction::getRemote, transfer -> new ArrayList<>(),
					ImportanceTransferRetriever::new, ImportanceTransferModelToDbModelMapping::new,
					ImportanceTransferDbModelToModelMapping::new, DbImportanceTransferTransaction.class,
					ImportanceTransferTransaction.class, "importanceTransferTransaction"));

			this.add(new Entry<>(TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					DbBlock::getBlockMultisigAggregateModificationTransactions, DbBlock::setBlockMultisigAggregateModificationTransactions,
					DbMultisigTransaction::getMultisigAggregateModificationTransaction,
					(multisig, t) -> multisig.setMultisigAggregateModificationTransaction((DbMultisigAggregateModificationTransaction) t),
					transfer -> null, transfer -> 1, transfer -> null, DbMultisigAggregateModificationTransaction::getOtherAccounts,
					MultisigModificationRetriever::new, MultisigAggregateModificationModelToDbModelMapping::new,
					MultisigAggregateModificationDbModelToModelMapping::new, DbMultisigAggregateModificationTransaction.class,
					MultisigAggregateModificationTransaction.class, "multisigAggregateModificationTransaction"));

			this.add(new Entry<>(TransactionTypes.MULTISIG, DbBlock::getBlockMultisigTransactions, DbBlock::setBlockMultisigTransactions,
					multisig -> null, null, DbModelUtils::getInnerTransaction,
					multisig -> 2 + multisig.getMultisigSignatureTransactions().size(), multisig -> null,
					DbMultisigTransaction::getOtherAccounts, MultisigTransactionRetriever::new,
					MultisigTransactionModelToDbModelMapping::new, MultisigTransactionDbModelToModelMapping::new,
					DbMultisigTransaction.class, org.nem.core.model.MultisigTransaction.class, null));

			this.add(new Entry<>(TransactionTypes.PROVISION_NAMESPACE, DbBlock::getBlockProvisionNamespaceTransactions,
					DbBlock::setBlockProvisionNamespaceTransactions, DbMultisigTransaction::getProvisionNamespaceTransaction,
					(multisig, t) -> multisig.setProvisionNamespaceTransaction((DbProvisionNamespaceTransaction) t), transfer -> null,
					transfer -> 1, transfer -> null, transfer -> Collections.singletonList(transfer.getRentalFeeSink()),
					ProvisionNamespaceRetriever::new, ProvisionNamespaceModelToDbModelMapping::new,
					ProvisionNamespaceDbModelToModelMapping::new, DbProvisionNamespaceTransaction.class,
					ProvisionNamespaceTransaction.class, "provisionNamespaceTransaction"));

			this.add(new Entry<>(TransactionTypes.MOSAIC_DEFINITION_CREATION, DbBlock::getBlockMosaicDefinitionCreationTransactions,
					DbBlock::setBlockMosaicDefinitionCreationTransactions, DbMultisigTransaction::getMosaicDefinitionCreationTransaction,
					(multisig, t) -> multisig.setMosaicDefinitionCreationTransaction((DbMosaicDefinitionCreationTransaction) t),
					transfer -> null, transfer -> 1, transfer -> null, transfer -> {
						final Collection<DbAccount> otherAccounts = new ArrayList<>();
						otherAccounts.add(transfer.getCreationFeeSink());

						final DbAccount feeRecipient = transfer.getMosaicDefinition().getFeeRecipient();
						if (null != feeRecipient && !feeRecipient.equals(transfer.getMosaicDefinition().getCreator())) {
							otherAccounts.add(feeRecipient);
						}

						return otherAccounts;
					}, MosaicDefinitionCreationRetriever::new, MosaicDefinitionCreationModelToDbModelMapping::new,
					MosaicDefinitionCreationDbModelToModelMapping::new, DbMosaicDefinitionCreationTransaction.class,
					MosaicDefinitionCreationTransaction.class, "mosaicDefinitionCreationTransaction"));

			this.add(new Entry<>(TransactionTypes.MOSAIC_SUPPLY_CHANGE, DbBlock::getBlockMosaicSupplyChangeTransactions,
					DbBlock::setBlockMosaicSupplyChangeTransactions, DbMultisigTransaction::getMosaicSupplyChangeTransaction,
					(multisig, t) -> multisig.setMosaicSupplyChangeTransaction((DbMosaicSupplyChangeTransaction) t), transfer -> null,
					transfer -> 1, transfer -> null, transfer -> Collections.emptyList(), MosaicSupplyChangeRetriever::new,
					MosaicSupplyChangeModelToDbModelMapping::new, MosaicSupplyChangeDbModelToModelMapping::new,
					DbMosaicSupplyChangeTransaction.class, MosaicSupplyChangeTransaction.class, "mosaicSupplyChangeTransaction"));
		}
	};

	/**
	 * Gets the number of entries.
	 *
	 * @return The number of entries.
	 */
	public static int size() {
		return ENTRIES.size();
	}

	/**
	 * Gets the number of entries that can be embedded in a multisig transaction.
	 *
	 * @return The number of entries.
	 */
	public static int multisigEmbeddableSize() {
		return size() - 1;
	}

	/**
	 * Gets all entries.
	 *
	 * @return The entries.
	 */
	@SuppressWarnings("unchecked")
	public static Iterable<Entry<AbstractBlockTransfer, Transaction>> iterate() {
		return () -> ENTRIES.stream().map(e -> (Entry<AbstractBlockTransfer, Transaction>) e).iterator();
	}

	/**
	 * Streams all entries.
	 *
	 * @return The entries.
	 */
	public static Stream<Entry<AbstractBlockTransfer, Transaction>> stream() {
		return StreamSupport.stream(iterate().spliterator(), false);
	}

	/**
	 * Finds an entry given a transaction type.
	 *
	 * @param type The transaction type.
	 * @return The entry.
	 */
	public static Entry<? extends AbstractBlockTransfer, ? extends Transaction> findByType(final Integer type) {
		for (final Entry<?, ?> entry : ENTRIES) {
			if (entry.type == type) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Finds an entry given a transaction db model class.
	 *
	 * @param clazz The db model class.
	 * @param <TDbModel> The type derived from AbstractBlockTransfer.
	 * @return The entry.
	 */
	@SuppressWarnings("unchecked")
	public static <TDbModel extends AbstractBlockTransfer> Entry<TDbModel, ?> findByDbModelClass(final Class<? extends TDbModel> clazz) {
		for (final Entry<?, ?> entry : ENTRIES) {
			if (entry.dbModelClass.equals(clazz)) {
				return (Entry<TDbModel, ?>) entry;
			}
		}

		return null;
	}
}
