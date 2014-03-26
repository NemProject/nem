package org.nem.core.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.dbmodel.Transfer;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

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
    public static org.nem.core.dbmodel.Block toDbModel(final Block block, final AccountDaoLookup accountDao) {
        final org.nem.core.dbmodel.Account forager = accountDao.findByAddress(block.getSigner().getAddress());

        final byte[] blockHash = HashUtils.calculateHash(block);
        final org.nem.core.dbmodel.Block dbBlock = new org.nem.core.dbmodel.Block(
            ByteUtils.bytesToLong(blockHash),
            block.getVersion(),
            block.getPreviousBlockHash(),
            blockHash,
            block.getTimeStamp().getRawTime(),
            forager,
            block.getSignature().getBytes(),
            block.getHeight(),
            0L,
            block.getTotalFee().getNumMicroNem());

        int i = 0;
        final List<Transfer> transactions = new ArrayList<>(block.getTransactions().size());
        for (final Transaction transaction : block.getTransactions()) {
            final Transfer dbTransfer = TransferMapper.toDbModel((TransferTransaction)transaction, i++, accountDao);
            dbTransfer.setBlock(dbBlock);
            transactions.add(dbTransfer);
        }

        dbBlock.setBlockTransfers(transactions);
        return dbBlock;
    }

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 *
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.core.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
        final Address foragerAddress = Address.fromPublicKey(dbBlock.getForger().getPublicKey());
		final Account forager = accountLookup.findByAddress(foragerAddress);

		final Block block = new org.nem.core.model.Block(
            forager,
            dbBlock.getPrevBlockHash(),
            new TimeInstant(dbBlock.getTimestamp()),
            dbBlock.getHeight());

		block.setSignature(new Signature(dbBlock.getForgerProof()));
		for (final Transfer dbTransfer : dbBlock.getBlockTransfers()) {
			final TransferTransaction transfer = TransferMapper.toModel(dbTransfer, accountLookup);
			block.addTransaction(transfer);
		}

		return block;
	}
}