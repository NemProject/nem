package org.nem.nis;

import org.nem.core.model.Block;
import org.nem.core.model.BlockHeight;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class BlockChainDbLayer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	final private AccountDao accountDao;
	final private BlockDao blockDao;
	private org.nem.nis.dbmodel.Block lastBlock;

	@Autowired(required = true)
	BlockChainDbLayer(final AccountDao accountDao, final BlockDao blockDao) {
		this.accountDao = accountDao;
		this.blockDao = blockDao;
	}

	public org.nem.nis.dbmodel.Block getLastDbBlock() {
		return lastBlock;
	}

	public Long getLastBlockHeight() {
		return lastBlock.getHeight();
	}

	public void analyzeLastBlock(org.nem.nis.dbmodel.Block curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		lastBlock = curBlock;
	}

	public boolean addBlockToDb(Block bestBlock) {
		synchronized (this) {

			final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(bestBlock, new AccountDaoLookupAdapter(this.accountDao));

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

	public void dropDbBlocksAfter(final BlockHeight height) {
		blockDao.deleteBlocksAfterHeight(height);
		lastBlock = blockDao.findByHeight(height);
	}

}
