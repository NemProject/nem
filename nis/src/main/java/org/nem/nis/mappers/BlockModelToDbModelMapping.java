package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.DbMultisigTransaction;

import java.util.*;

/**
 * A mapping that is able to map a model block to a db block.
 */
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
		final DbAccount lessor = null != block.getLessor()
				? this.mapper.map(block.getLessor(), DbAccount.class)
				: null;

		final Hash blockHash = HashUtils.calculateHash(block);
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setBlockHash(blockHash);
		dbBlock.setVersion(block.getVersion());
		dbBlock.setGenerationHash(block.getGenerationHash());
		dbBlock.setPrevBlockHash(block.getPreviousBlockHash());
		dbBlock.setTimeStamp(block.getTimeStamp().getRawTime());
		dbBlock.setForger(harvester);
		dbBlock.setForgerProof(block.getSignature().getBytes());
		dbBlock.setHeight(block.getHeight().getRaw());
		dbBlock.setTotalFee(block.getTotalFee().getNumMicroNem());
		dbBlock.setDifficulty(block.getDifficulty().getRaw());
		dbBlock.setLessor(lessor);

		final BlockTransactionProcessor processor = new BlockTransactionProcessor(mapper, dbBlock);
		block.getTransactions().forEach(processor::process);
		processor.commit();
		return dbBlock;
	}

	private static class BlockTransactionContext<T extends AbstractBlockTransfer> {
		public final int type;
		private final List<T> transactions = new ArrayList<>();
		private final TransactionRegistry.Entry<T, ?> entry;
		public int nextIndex;

		@SuppressWarnings("unchecked")
		public BlockTransactionContext(final int type) {
			this.entry = (TransactionRegistry.Entry<T, ?>)TransactionRegistry.findByType(type);
			this.type = type;
		}

		public T mapAndAdd(final IMapper mapper, final Transaction transaction) {
			final T dbTransfer = mapper.map(transaction, this.entry.dbModelClass);
			this.transactions.add(dbTransfer);
			return dbTransfer;
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
			final BlockTransactionContext<?> context = this.transactionContexts.getOrDefault(transaction.getType(), null);
			if (null == context) {
				throw new IllegalArgumentException("trying to map block with unknown transaction type");
			}

			final AbstractBlockTransfer dbTransfer = context.mapAndAdd(this.mapper, transaction);
			dbTransfer.setOrderId(context.nextIndex++);
			dbTransfer.setBlkIndex(this.blockIndex);
			dbTransfer.setBlock(this.dbBlock);

			if (dbTransfer instanceof DbMultisigTransaction) {
				final DbMultisigTransaction dbMultisigTransfer = (DbMultisigTransaction)dbTransfer;
				for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
					final AbstractBlockTransfer dbInnerTransfer = entry.getFromMultisig.apply(dbMultisigTransfer);
					if (null == dbInnerTransfer) {
						continue;
					}

					dbInnerTransfer.setOrderId(-1);
					dbInnerTransfer.setBlkIndex(this.blockIndex);
					// TODO 20150105 G: probably doesn't make sense to do it
					// TODO 20150105 J-G: you mean don't set the block on the inner transfer?
					dbInnerTransfer.setBlock(this.dbBlock);
				}
			}

			++this.blockIndex;
		}

		public void commit() {
			for (final BlockTransactionContext<?> context : this.transactionContexts.values()) {
				context.commit(this.dbBlock);
			}
		}
	}
}