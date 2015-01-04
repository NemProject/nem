package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.dbmodel.*;
import org.nem.nis.dbmodel.MultisigTransaction;

import java.util.*;

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

		final BlockTransactionDbMapper blockTransactionDbMapper = new BlockTransactionDbMapper(mapper, dbBlock, block.getTransactions().size());
		for (final Transaction transaction : block.getTransactions()) {
			blockTransactionDbMapper.handleTransaction(transaction);
		}
		blockTransactionDbMapper.saveTransfers();

		return dbBlock;
	}


	private static class BlockTransactionDbMapper {
		private final org.nem.nis.dbmodel.Block dbBlock;
		private final IMapper mapper;

		final List<ImportanceTransfer> importanceTransferTransactions = new ArrayList<>(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK / 10);
		final List<MultisigSignerModification> multisigSignerModificationsTransactions = new ArrayList<>(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK / 10);
		final List<MultisigTransaction> multisigTransactions = new ArrayList<>(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK / 10);
		final List<Transfer> transferTransactions;

		int i = 0;
		int multisigSignerModificationsIndex = 0;
		int multisigTransactionsIndex = 0;
		int importanceTransferIndex = 0;
		int transferIndex = 0;

		private BlockTransactionDbMapper(final IMapper mapper, final org.nem.nis.dbmodel.Block dbBlock, final int initialCapacity) {
			this.mapper = mapper;
			this.dbBlock = dbBlock;
			this.transferTransactions = new ArrayList<>(initialCapacity);
		}

		private MultisigTransaction handleMultisig(final Transaction transaction) {
			final MultisigTransaction dbTransfer = this.mapper.map(transaction, MultisigTransaction.class);
			dbTransfer.setOrderId(this.multisigTransactionsIndex++);
			dbTransfer.setBlkIndex(this.i);
			dbTransfer.setBlock(this.dbBlock);
			this.multisigTransactions.add(dbTransfer);

			return dbTransfer;
		}

		private void handleTransaction(final Transaction transaction) {
			this.handleTransaction(transaction, null);
		}

		private void handleTransaction(final Transaction transaction, final MultisigTransaction multisig) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final Transfer dbTransfer = this.mapper.map(transaction, Transfer.class);
					dbTransfer.setOrderId(this.transferIndex++);
					dbTransfer.setBlkIndex(this.i);
					dbTransfer.setBlock(this.dbBlock);
					this.transferTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setTransfer(dbTransfer);
					}
				}
				break;

				case TransactionTypes.IMPORTANCE_TRANSFER: {
					final ImportanceTransfer dbTransfer = this.mapper.map(transaction, ImportanceTransfer.class);
					dbTransfer.setOrderId(this.importanceTransferIndex++);
					dbTransfer.setBlkIndex(this.i);
					dbTransfer.setBlock(this.dbBlock);
					this.importanceTransferTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setImportanceTransfer(dbTransfer);
					}
				}
				break;
				case TransactionTypes.MULTISIG_SIGNER_MODIFY: {
					final MultisigSignerModification dbTransfer = this.mapper.map(transaction, MultisigSignerModification.class);
					dbTransfer.setOrderId(this.multisigSignerModificationsIndex++);
					dbTransfer.setBlkIndex(this.i);
					dbTransfer.setBlock(this.dbBlock);
					this.multisigSignerModificationsTransactions.add(dbTransfer);

					if (multisig != null) {
						multisig.setMultisigSignerModification(dbTransfer);
					}
				}
				break;
				case TransactionTypes.MULTISIG: {
					if (multisig != null) {
						throw new RuntimeException("multisig inside multisig");
					}

					final MultisigTransaction multisigTransaction = this.handleMultisig(transaction);
					// recursive call
					this.handleTransaction(((org.nem.core.model.MultisigTransaction) transaction).getOtherTransaction(), multisigTransaction);
				}
				break;
				default:
					throw new RuntimeException("trying to map block with unknown transaction type");
			}

			// if current transaction is part of multisig transaction we will NOT increment blockindex
			if (multisig == null) {
				this.i++;
			}
		}

		public void saveTransfers() {
			this.dbBlock.setBlockTransfers(this.transferTransactions);
			this.dbBlock.setBlockImportanceTransfers(this.importanceTransferTransactions);
			this.dbBlock.setBlockMultisigSignerModifications(this.multisigSignerModificationsTransactions);
			this.dbBlock.setBlockMultisigTransactions(this.multisigTransactions);
		}
	}
}
