package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * Static class that contains functions for converting to and from
 * db-model Block and model Block.
 */
public class BlockMapper {

	/**
	 * Converts a Block model to a Block db-model.
	 *
	 * @param block The block model.
	 * @param accountDao The account dao lookup object.
	 * @return The Block db-model.
	 */
	public static org.nem.nis.dbmodel.Block toDbModel(final Block block, final AccountDaoLookup accountDao) {
		final org.nem.nis.dbmodel.Account harvester = accountDao.findByAddress(block.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account lessor = block.getLessor() != null ? accountDao.findByAddress(block.getLessor().getAddress()) : null;

		final Hash blockHash = HashUtils.calculateHash(block);
		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block(
				blockHash,
				block.getVersion(),
				block.getGenerationHash(),
				block.getPreviousBlockHash(),
				block.getTimeStamp().getRawTime(),
				harvester,
				block.getSignature().getBytes(),
				block.getHeight().getRaw(),
				0L,
				block.getTotalFee().getNumMicroNem(),
				block.getDifficulty().getRaw(),
				lessor);

		// TODO 20140923 J-G [QUESTION] but any reason you didn't want to have a transfer hierarchy in the db?
		// > something like class table inheritance: http://stackoverflow.com/tags/class-table-inheritance/info
		// > i'm not a db expert by any stretch, so i'm not opposed to what you did, just curious why you chose it;
		// > i guess performance is the main benefit?
		// > does it make sense to consider having something like a hash table so we can query a single table to
		// > see if a transaction exists instead of N tables (not a big deal now since there are only 2
		// > transaction types, but might become more important as N grows)
		//
		// It is possible to do class hierarchy using hibernate annotations.
		// Let's assume that in dbmodel we would have Transaction, and Transfer that extends Transaction
		// and ImportanceTransfer that extends Transaction.
		//
		// Now there is a way to tell hibernate it would hold List<> of Transactions.
		// (Transaction would be @MappedSuperclass with InheritanceType.JOINED)
		//
		// I have a feeling we would sooner or later run into problems...
		// performance is the other reason, haven't tried it, but I guess queries would be quite complicated
		// (although I might be wrong on this).
		//
		// (slightly off-topic, since we want to sort TXes by fees, code below will be changed)
		//
		int i = 0;
		final List<Transfer> transferTransactions = new ArrayList<>(block.getTransactions().size());
		final List<ImportanceTransfer> importanceTransferTransactions = new ArrayList<>(block.getTransactions().size());
		for (final Transaction transaction : block.getTransactions()) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final Transfer dbTransfer = TransferMapper.toDbModel((TransferTransaction)transaction, i++, accountDao);
					dbTransfer.setBlock(dbBlock);
					transferTransactions.add(dbTransfer);
				}
				break;
				case TransactionTypes.IMPORTANCE_TRANSFER: {
					final ImportanceTransfer dbTransfer = ImportanceTransferMapper.toDbModel((ImportanceTransferTransaction)transaction, i++, accountDao);
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

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		if (1 == dbBlock.getHeight()) {
			return NemesisBlock.fromResource(new DeserializationContext(accountLookup));
		}

		final Address foragerAddress = Address.fromPublicKey(dbBlock.getForger().getPublicKey());
		final Account forager = accountLookup.findByAddress(foragerAddress);
		final Address lessorAddress = dbBlock.getLessor() != null ? Address.fromPublicKey(dbBlock.getLessor().getPublicKey()) : null;
		final Account lessor = lessorAddress != null ? accountLookup.findByAddress(lessorAddress) : null;

		final Block block = new org.nem.core.model.Block(
				forager,
				dbBlock.getPrevBlockHash(),
				dbBlock.getGenerationHash(),
				new TimeInstant(dbBlock.getTimeStamp()),
				new BlockHeight(dbBlock.getHeight()));

		final Long difficulty = dbBlock.getDifficulty();
		block.setDifficulty(new BlockDifficulty(null == difficulty ? 0L : difficulty));
		block.setLessor(lessor);
		block.setSignature(new Signature(dbBlock.getForgerProof()));

		// TODO 20140921 J-G: not sure if this is a test thing or not, but a number of tests were failing because dbBlock.getBlockImportanceTransfers() was null
		if (null != dbBlock.getBlockImportanceTransfers()) {
			for (final ImportanceTransfer dbTransfer : dbBlock.getBlockImportanceTransfers()) {
				final ImportanceTransferTransaction importanceTransferTransaction = ImportanceTransferMapper.toModel(dbTransfer, accountLookup);
				block.addTransaction(importanceTransferTransaction);
			}
		}

		for (final Transfer dbTransfer : dbBlock.getBlockTransfers()) {
			final TransferTransaction transfer = TransferMapper.toModel(dbTransfer, accountLookup);
			block.addTransaction(transfer);
		}

		return block;
	}
}