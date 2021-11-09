package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * A mapping that is able to map a model block to a db block.
 */
@SuppressWarnings("rawtypes")
public class BlockModelToDbModelMapping implements IMapping<Block, DbBlock> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public BlockModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbBlock map(final Block block) {
		final DbAccount harvester = this.mapper.map(block.getSigner(), DbAccount.class);
		final DbAccount lessor = null != block.getLessor() ? this.mapper.map(block.getLessor(), DbAccount.class) : null;

		final Hash blockHash = HashUtils.calculateHash(block);
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setBlockHash(blockHash);
		dbBlock.setVersion(block.getVersion());
		dbBlock.setGenerationHash(block.getGenerationHash());
		dbBlock.setPrevBlockHash(block.getPreviousBlockHash());
		dbBlock.setTimeStamp(block.getTimeStamp().getRawTime());
		dbBlock.setHarvester(harvester);
		dbBlock.setHarvesterProof(block.getSignature().getBytes());
		dbBlock.setHeight(block.getHeight().getRaw());
		dbBlock.setTotalFee(block.getTotalFee().getNumMicroNem());
		dbBlock.setDifficulty(block.getDifficulty().getRaw());
		dbBlock.setLessor(lessor);

		final BlockTransactionProcessor processor = new BlockTransactionProcessor(this.mapper, dbBlock);
		block.getTransactions().forEach(processor::process);
		processor.commit();
		return dbBlock;
	}

	private static class BlockTransactionContext<T extends AbstractBlockTransfer> {
		private final List<T> transactions = new ArrayList<>();
		private final TransactionRegistry.Entry<T, ?> entry;

		@SuppressWarnings("unchecked")
		public BlockTransactionContext(final int type) {
			this.entry = (TransactionRegistry.Entry<T, ?>) TransactionRegistry.findByType(type);
		}

		public T mapAndAdd(final IMapper mapper, final Transaction transaction) {
			final T dbTransfer = mapper.map(transaction, this.entry.dbModelClass);
			this.transactions.add(dbTransfer);
			return dbTransfer;
		}

		@SuppressWarnings("unchecked")
		public void add(final AbstractBlockTransfer dbTransfer) {
			this.transactions.add((T) dbTransfer);
		}

		public void commit(final DbBlock dbBlock) {
			this.entry.setInBlock.accept(dbBlock, this.transactions);
		}
	}

	private static class BlockTransactionProcessor {
		private final IMapper mapper;
		private final DbBlock dbBlock;
		private final Map<Integer, BlockTransactionContext> transactionContexts;
		private int blockIndex;

		public BlockTransactionProcessor(final IMapper mapper, final DbBlock dbBlock) {
			this.mapper = mapper;
			this.dbBlock = dbBlock;

			this.transactionContexts = new HashMap<>();
			for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
				this.transactionContexts.put(entry.type, new BlockTransactionContext<>(entry.type));
			}
		}

		public void process(final Transaction transaction) {
			final BlockTransactionContext<?> context = this.getContext(transaction.getType());

			final AbstractBlockTransfer dbTransfer = context.mapAndAdd(this.mapper, transaction);
			dbTransfer.setBlkIndex(this.blockIndex);
			dbTransfer.setBlock(this.dbBlock);

			if (dbTransfer instanceof DbMultisigTransaction) {
				final DbMultisigTransaction dbMultisigTransfer = (DbMultisigTransaction) dbTransfer;
				for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
					final AbstractBlockTransfer dbInnerTransfer = entry.getFromMultisig.apply(dbMultisigTransfer);
					if (null == dbInnerTransfer) {
						continue;
					}

					final BlockTransactionContext<?> innerContext = this.getContext(entry.type);
					dbInnerTransfer.setBlkIndex(this.blockIndex);
					dbInnerTransfer.setBlock(this.dbBlock);
					innerContext.add(dbInnerTransfer);
				}
			}

			++this.blockIndex;
		}

		private BlockTransactionContext<?> getContext(final int transactionType) {
			final BlockTransactionContext<?> context = this.transactionContexts.getOrDefault(transactionType, null);
			if (null == context) {
				throw new IllegalArgumentException("trying to map block with unknown transaction type");
			}

			return context;
		}

		public void commit() {
			for (final BlockTransactionContext<?> context : this.transactionContexts.values()) {
				context.commit(this.dbBlock);
			}
		}
	}
}
