package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

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

		int i = 0;
		int importanceTransferIndex = 0;
		int transferIndex = 0;
		final int numTransactions = block.getTransactions().size();
		final List<Transfer> transferTransactions = new ArrayList<>(numTransactions);
		final List<ImportanceTransfer> importanceTransferTransactions = new ArrayList<>(numTransactions);
		for (final Transaction transaction : block.getTransactions()) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final Transfer dbTransfer = this.mapper.map(transaction, Transfer.class);
					dbTransfer.setOrderId(transferIndex++);
					dbTransfer.setBlkIndex(i++);
					dbTransfer.setBlock(dbBlock);
					transferTransactions.add(dbTransfer);
				}
				break;

				case TransactionTypes.IMPORTANCE_TRANSFER: {
					final ImportanceTransfer dbTransfer = this.mapper.map(transaction, ImportanceTransfer.class);
					dbTransfer.setOrderId(importanceTransferIndex++);
					dbTransfer.setBlkIndex(i++);
					dbTransfer.setBlock(dbBlock);
					importanceTransferTransactions.add(dbTransfer);
				}
				break;
			}
		}

		dbBlock.setBlockTransfers(transferTransactions);
		dbBlock.setBlockImportanceTransfers(importanceTransferTransactions);
		return dbBlock;
	}
}
