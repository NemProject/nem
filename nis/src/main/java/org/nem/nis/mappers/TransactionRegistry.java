package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.Block;

import java.util.*;
import java.util.function.*;

/**
 * Contains transaction mapping metadata.
 */
public class TransactionRegistry {

	/**
	 * A registry entry.
	 */
	public static class Entry<TDbModel extends AbstractTransfer, TModel extends Transaction> {

		/**
		 * A function that will return db model transactions given a block
		 */
		public final Function<Block, List<TDbModel>> getFromBlock;

		/**
		 * A function that will return set db model transactions given a block
		 */
		public final BiConsumer<Block, List<TDbModel>> setInBlock;

		/**
		 * A function that will create a model to db model transaction mapping given a mapper.
		 */
		public final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper;

		/**
		 * A function that will create a db model to model transaction mapping given a mapper.
		 */
		public final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper;

		/**
		 * The db model transaction class.
		 */
		public final Class<TDbModel> dbModelClass;

		/**
		 * The model transaction class.
		 */
		public final Class<TModel> modelClass;

		private Entry(
				final Function<Block, List<TDbModel>> getFromBlock,
				final BiConsumer<Block, List<TDbModel>> setInBlock,
				final Function<IMapper, IMapping<TModel, TDbModel>> createModelToDbModelMapper,
				final Function<IMapper, IMapping<TDbModel, TModel>> createDbModelToModelMapper,
				final Class<TDbModel> dbModelClass,
				final Class<TModel> modelClass) {
			this.getFromBlock = getFromBlock;
			this.setInBlock = setInBlock;
			this.createModelToDbModelMapper = createModelToDbModelMapper;
			this.createDbModelToModelMapper = createDbModelToModelMapper;
			this.dbModelClass = dbModelClass;
			this.modelClass = modelClass;
		}
	}

	private static final List<Entry<?, ?>> entries = new ArrayList<Entry<?, ?>>() {
		{
			this.add(new Entry<>(
					Block::getBlockTransfers,
					(block, transfers) -> block.setBlockTransfers(transfers),
					TransferModelToDbModelMapping::new,
					TransferDbModelToModelMapping::new,
					Transfer.class,
					TransferTransaction.class));
			this.add(new Entry<>(
					Block::getBlockImportanceTransfers,
					(block, transfers) -> block.setBlockImportanceTransfers(transfers),
					ImportanceTransferModelToDbModelMapping::new,
					ImportanceTransferDbModelToModelMapping::new,
					ImportanceTransfer.class,
					ImportanceTransferTransaction.class));
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
	 * Gets all entries.
	 */
	public static Iterable<Entry<?, ?>> iterate() {
		return entries;
	}
}
