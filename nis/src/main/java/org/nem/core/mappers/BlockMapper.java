package org.nem.core.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.dao.AccountDao;
import org.nem.core.dbmodel.Transfer;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.AccountAnalyzer;

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
     * @param accountDao The account data access object.
     * @return The Block db-model.
     */
    public static org.nem.core.dbmodel.Block toDbModel(final Block block, final AccountDao accountDao) {
        final org.nem.core.dbmodel.Account forager = getAccountDbModel(block.getSigner(), accountDao);

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

    private static org.nem.core.dbmodel.Account getAccountDbModel(final Account account, final AccountDao accountDao) {
        return accountDao.getAccountByPrintableAddress(account.getAddress().getEncoded());
    }

	/**
	 * Converts Block db-model to Block model
	 *
	 * @param block db-model block orm object
	 * @param accountAnalyzer analyzer containing information about accounts.
	 *
	 * @return Block model.
	 */
	public static Block toModel(final org.nem.core.dbmodel.Block block, final AccountLookup accountAnalyzer) {
		org.nem.core.model.Account forager = accountAnalyzer.findByAddress(Address.fromPublicKey(block.getForger().getPublicKey()));
		org.nem.core.model.Block res = new org.nem.core.model.Block(
				forager,
				block.getPrevBlockHash(),
				new TimeInstant(block.getTimestamp()),
				block.getHeight()
		);
		res.setSignature(new Signature(block.getForgerProof()));
		for (Transfer transfer : block.getBlockTransfers()) {
			final TransferTransaction transferTransaction = TransferMapper.toModel(transfer, accountAnalyzer);
			res.addTransaction(transferTransaction);
		}

		return res;
	}
}