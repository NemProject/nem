package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.dbmodel.MultisigTransaction;

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
		public final Function<Block, List<TDbModel>> getFromBlock;

		/**
		 * A function that will return set db model transactions given a block
		 */
		public final BiConsumer<Block, List<TDbModel>> setInBlock;

		/**
		 * A function that will get db model transactions given a multisig transfer.
		 */
		public final Function<MultisigTransaction, TDbModel> getFromMultisig;

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
				final Function<Block, List<TDbModel>> getFromBlock,
				final BiConsumer<Block, List<TDbModel>> setInBlock,
				final Function<MultisigTransaction, TDbModel> getFromMultisig,
				final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper,
				final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper,
				final Class<TDbModel> dbModelClass,
				final Class<TModel> modelClass) {
			this.type = type;

			this.getFromBlock = getFromBlock;
			this.setInBlock = setInBlock;

			this.getFromMultisig = getFromMultisig;

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
					Block::getBlockTransfers,
					(block, transfers) -> block.setBlockTransfers(transfers),
					MultisigTransaction::getTransfer,
					TransferModelToDbModelMapping::new,
					TransferDbModelToModelMapping::new,
					Transfer.class,
					TransferTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.IMPORTANCE_TRANSFER,
					Block::getBlockImportanceTransfers,
					(block, transfers) -> block.setBlockImportanceTransfers(transfers),
					MultisigTransaction::getImportanceTransfer,
					ImportanceTransferModelToDbModelMapping::new,
					ImportanceTransferDbModelToModelMapping::new,
					ImportanceTransfer.class,
					ImportanceTransferTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION,
					Block::getBlockMultisigSignerModifications,
					(block, transfers) -> block.setBlockMultisigSignerModifications(transfers),
					MultisigTransaction::getMultisigSignerModification,
					MultisigSignerModificationModelToDbModelMapping::new,
					MultisigSignerModificationDbModelToModelMapping::new,
					MultisigSignerModification.class,
					MultisigAggregateModificationTransaction.class));

			this.add(new Entry<>(
					TransactionTypes.MULTISIG,
					Block::getBlockMultisigTransactions,
					(block, transfers) -> block.setBlockMultisigTransactions(transfers),
					multisig -> null,
					MultisigTransactionModelToDbModelMapping::new,
					MultisigTransactionDbModelToModelMapping::new,
					MultisigTransaction.class,
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
}
