package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.MultisigTransaction;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A mapping that is able to map a model block to a db block.
 */
public class BlockModelToDbModelMapping implements IMapping<Block, org.nem.nis.dbmodel.Block> {
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
	public org.nem.nis.dbmodel.Block map(final Block block) {
		final org.nem.nis.dbmodel.Account harvester = this.mapper.map(block.getSigner(), org.nem.nis.dbmodel.Account.class);
		final org.nem.nis.dbmodel.Account lessor = null != block.getLessor()
				? this.mapper.map(block.getLessor(), org.nem.nis.dbmodel.Account.class)
				: null;

		final Hash blockHash = HashUtils.calculateHash(block);
		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();
		dbBlock.setBlockHash(blockHash);
		dbBlock.setVersion(block.getVersion());
		dbBlock.setGenerationHash(block.getGenerationHash());
		dbBlock.setPrevBlockHash(block.getPreviousBlockHash());
		dbBlock.setTimeStamp(block.getTimeStamp().getRawTime());
		dbBlock.setForger(harvester);
		dbBlock.setForgerProof(block.getSignature().getBytes());
		dbBlock.setHeight(block.getHeight().getRaw());
		dbBlock.setTotalAmount(0L); // TODO 20141227 J-G: we can probably remove this from the database as well?
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
		private final BiConsumer<org.nem.nis.dbmodel.Block, List<T>> setInBlock;
		private final Class<T> dbModelType;
		private final List<T> transactions = new ArrayList<>();
		public int nextIndex;

		public BlockTransactionContext(
				final int type,
				final BiConsumer<org.nem.nis.dbmodel.Block, List<T>> setInBlock,
				final Class<T> dbModelType) {
			this.type = type;
			this.setInBlock = setInBlock;
			this.dbModelType = dbModelType;
		}

		public T mapAndAdd(final IMapper mapper, final Transaction transaction) {
			final T dbTransfer = mapper.map(transaction, this.dbModelType);
			this.transactions.add(dbTransfer);
			return dbTransfer;
		}

		public void commit(final org.nem.nis.dbmodel.Block dbBlock) {
			this.setInBlock.accept(dbBlock, this.transactions);
		}
	}

	private static class BlockTransactionProcessor {
		private final IMapper mapper;
		private final org.nem.nis.dbmodel.Block dbBlock;
		private int blockIndex;

		// TODO 20150104 J-J: move to registry (hopefully)
		private final Map<Integer, BlockTransactionContext> transactionContexts = new HashMap<Integer, BlockTransactionContext>() {
			{
				this.put(
						TransactionTypes.TRANSFER,
						new BlockTransactionContext<>(
								TransactionTypes.TRANSFER,
								(b, t) -> b.setBlockTransfers(t),
								Transfer.class));
				this.put(
						TransactionTypes.IMPORTANCE_TRANSFER,
						new BlockTransactionContext<>(
								TransactionTypes.IMPORTANCE_TRANSFER,
								(b, t) -> b.setBlockImportanceTransfers(t),
								ImportanceTransfer.class));
				this.put(
						TransactionTypes.MULTISIG_SIGNER_MODIFY,
						new BlockTransactionContext<>(
								TransactionTypes.MULTISIG_SIGNER_MODIFY, (b, t) ->
								b.setBlockMultisigSignerModifications(t),
								MultisigSignerModification.class));
				this.put(
						TransactionTypes.MULTISIG,
						new BlockTransactionContext<>(
								TransactionTypes.MULTISIG,
								(b, t) -> b.setBlockMultisigTransactions(t),
								MultisigTransaction.class));
			}
		};

		public BlockTransactionProcessor(final IMapper mapper, final org.nem.nis.dbmodel.Block dbBlock) {
			this.mapper = mapper;
			this.dbBlock = dbBlock;
		}

		public void process(final Transaction transaction) {
			final BlockTransactionContext<?> context = this.transactionContexts.getOrDefault(transaction.getType(), null);
			if (null == context) {
				// TODO 20150104 J-J: need to test this
				throw new RuntimeException("trying to map block with unknown transaction type");
			}

			final AbstractBlockTransfer dbTransfer = context.mapAndAdd(this.mapper, transaction);
			dbTransfer.setOrderId(context.nextIndex++);
			dbTransfer.setBlkIndex(this.blockIndex++);
			dbTransfer.setBlock(this.dbBlock);

			// TODO 20150104: need to set same fields on inner transactions!
		}

		public void commit() {
			for (final BlockTransactionContext<?> context : this.transactionContexts.values()) {
				context.commit(this.dbBlock);
			}
		}
	}
}