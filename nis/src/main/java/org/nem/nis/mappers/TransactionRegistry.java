package org.nem.nis.mappers;

import org.nem.core.function.PentaFunction;
import org.nem.core.model.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.*;

/**
 * Contains transaction mapping metadata.
 */
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
		 * A function that will get the inner transaction or null if none is available.
		 */
		public final Function<TDbModel, AbstractBlockTransfer> getInnerTransaction;

		/**
		 * A function that will get the number of transactions involved in the given transaction.
		 */
		public final Function<TDbModel, Integer> getTransactionCount;

		/**
		 * A function that will get the recipient (if any) given an abstract block transfer.
		 */
		public final Function<TDbModel, DbAccount> getRecipient;

		/**
		 * A function that will get a list of db accounts (if any) given an abstract block transfer.
		 */
		public final Function<TDbModel, Collection<DbAccount>> getOtherAccounts;

		/**
		 * A function that will return transfer block pairs from the database.
		 */
		public final PentaFunction<TransferDao, Long, Long, Integer, ReadOnlyTransferDao.TransferType, Collection<TransferBlockPair>> getFromDb;

		/**
		 * The db model transaction class.
		 */
		public final Class<TDbModel> dbModelClass;

		/**
		 * The model transaction class.
		 */
		public final Class<TModel> modelClass;

		private final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper;
		private final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper;

		private Entry(
				final int type,
				final Function<DbBlock, List<TDbModel>> getFromBlock,
				final BiConsumer<DbBlock, List<TDbModel>> setInBlock,
				final Function<DbMultisigTransaction, TDbModel> getFromMultisig,
				final Function<TDbModel, AbstractBlockTransfer> getInnerTransaction,
				final Function<TDbModel, Integer> getTransactionCount,
				final Function<TDbModel, DbAccount> getRecipient,
				final Function<TDbModel, Collection<DbAccount>> getOtherAccounts,
				final PentaFunction<TransferDao, Long, Long, Integer, ReadOnlyTransferDao.TransferType, Collection<TransferBlockPair>> getFromDb,
				final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper,
				final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper,
				final Class<TDbModel> dbModelClass,
				final Class<TModel> modelClass) {
			this.type = type;

			this.getFromBlock = getFromBlock;
			this.setInBlock = setInBlock;

			this.getFromMultisig = getFromMultisig;
			this.getInnerTransaction = getInnerTransaction;
			this.getTransactionCount = getTransactionCount;

			this.getRecipient = getRecipient;
			this.getOtherAccounts = getOtherAccounts;

			this.getFromDb = getFromDb;

			this.createModelToDbModelMapper = createModelToDbModelMapper;
			this.createDbModelToModelMapper = createDbModelToModelMapper;
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
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

	private static final List<Entry<?, ?>> entries = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(
					TransactionTypes.TRANSFER,
					DbBlock::getBlockTransferTransactions,
					DbBlock::setBlockTransferTransactions,
					DbMultisigTransaction::getTransferTransaction,
					transfer -> null,
					transfer -> 1,
					DbTransferTransaction::getRecipient,
					transfer -> new ArrayList<>(),
					ReadOnlyTransferDao::getTransfersForAccount,
					TransferModelToDbModelMapping::new,
					TransferDbModelToModelMapping::new,
					DbTransferTransaction.class,
					TransferTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.IMPORTANCE_TRANSFER,
					DbBlock::getBlockImportanceTransferTransactions,
					DbBlock::setBlockImportanceTransferTransactions,
					DbMultisigTransaction::getImportanceTransferTransaction,
					transfer -> null,
					transfer -> 1,
					DbImportanceTransferTransaction::getRemote,
					transfer -> new ArrayList<>(),
					ReadOnlyTransferDao::getImportanceTransfersForAccount,
					ImportanceTransferModelToDbModelMapping::new,
					ImportanceTransferDbModelToModelMapping::new,
					DbImportanceTransferTransaction.class,
					ImportanceTransferTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					DbBlock::getBlockMultisigAggregateModificationTransactions,
					DbBlock::setBlockMultisigAggregateModificationTransactions,
					DbMultisigTransaction::getMultisigAggregateModificationTransaction,
					transfer -> null,
					transfer -> 1,
					transfer -> null,
					DbMultisigAggregateModificationTransaction::getOtherAccounts,
					ReadOnlyTransferDao::getMultisigSignerModificationsForAccount,
					MultisigAggregateModificationModelToDbModelMapping::new,
					MultisigAggregateModificationDbModelToModelMapping::new,
					DbMultisigAggregateModificationTransaction.class,
					MultisigAggregateModificationTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG,
					DbBlock::getBlockMultisigTransactions,
					DbBlock::setBlockMultisigTransactions,
					multisig -> null,
					DbModelUtils::getInnerTransaction,
					multisig -> 2 + multisig.getMultisigSignatureTransactions().size(),
					multisig -> null,
					DbMultisigTransaction::getOtherAccounts,
					ReadOnlyTransferDao::getMultisigTransactionsForAccount,
					MultisigTransactionModelToDbModelMapping::new,
					MultisigTransactionDbModelToModelMapping::new,
					DbMultisigTransaction.class,
					org.nem.core.model.MultisigTransaction.class));
		}
	};

	/**
	 * Gets the number of entries.
	 *
	 * @return The number of entries.
	 */
	public static int size() {
		return entries.size();
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
	public static Iterable<Entry<?, ?>> iterate() {
		return entries;
	}

	/**
	 * Finds an entry given a transaction type.
	 *
	 * @param type The transaction type.
	 * @return The entry.
	 */
	public static Entry<?, ?> findByType(final Integer type) {
		for (final Entry<?, ?> entry : entries) {
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
	 * @return The entry.
	 */
	public static <TDbModel extends AbstractBlockTransfer> Entry<AbstractBlockTransfer, ?> findByDbModelClass(final Class<TDbModel> clazz) {
		for (final Entry<?, ?> entry : entries) {
			if (entry.dbModelClass.equals(clazz)) {
				return (Entry<AbstractBlockTransfer, ?>)entry;
			}
		}

		return null;
	}
}
