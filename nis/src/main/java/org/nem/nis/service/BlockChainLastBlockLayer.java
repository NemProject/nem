package org.nem.nis.service;

import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.BlockHeight;
import org.nem.nis.BlockChain;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/**
 * This is intermediate layer between blocchain or foraging and actual Dao
 *
 * TODO: not sure if other dau functions should be moved here, probably not
 */
@Service
public class BlockChainLastBlockLayer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	final private AccountDao accountDao;
	final private BlockDao blockDao;

	private org.nem.nis.dbmodel.Block lastBlock;

	@Autowired(required = true)
	public BlockChainLastBlockLayer(final AccountDao accountDao, final BlockDao blockDao) {
		this.accountDao = accountDao;
		this.blockDao = blockDao;
	}

	/**
	 * Returns last block in the db.
	 * @return last block from db.
	 */
	public org.nem.nis.dbmodel.Block getLastDbBlock() {
		return lastBlock;
	}

	/**
	 * Returns height of last block in the db.
	 * @return height of last block in the db.
	 */
	public Long getLastBlockHeight() {
		return lastBlock.getHeight();
	}

	/**
	 * Analyzes last block, used during initial initialization of blocks in blockchain.
	 * @param curBlock lastBlock in db.
	 */
	public void analyzeLastBlock(org.nem.nis.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;
	}

	/**
	 * Adds new block into db
	 *
	 * @param block block to be added to db
	 * @return always true
	 */
	public boolean addBlockToDb(Block block) {
		synchronized (this) {

			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));

			// hibernate will save both block AND transactions
			// as there is cascade in Block
			// mind that there is NO cascade in transaction (near block field)
			blockDao.save(dbBlock);

			lastBlock.setNextBlockId(dbBlock.getId());
			blockDao.updateLastBlockId(lastBlock);

			lastBlock = dbBlock;

		} // synchronized

		return true;
	}

	/**
	 * Removes block from the db after specified height
	 */
	public void dropDbBlocksAfter(final BlockHeight height) {
		blockDao.deleteBlocksAfterHeight(height);
		lastBlock = blockDao.findByHeight(height);
	}

}
