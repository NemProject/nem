package org.nem.nis.service;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChain;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisModelToDbModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/**
 * This is intermediate layer between blockchain or harvesting and actual Dao.
 */
@Service
public class BlockChainLastBlockLayer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	private final BlockDao blockDao;
	private final NisModelToDbModelMapper mapper;

	private DbBlock lastBlock;

	@Autowired(required = true)
	public BlockChainLastBlockLayer(final BlockDao blockDao, final NisModelToDbModelMapper mapper) {
		this.blockDao = blockDao;
		this.mapper = mapper;
	}

	/**
	 * Returns last block in the db.
	 *
	 * @return last block from db.
	 */
	public DbBlock getLastDbBlock() {
		return this.lastBlock;
	}

	/**
	 * Returns height of last block in the db.
	 *
	 * @return height of last block in the db.
	 */
	public Long getLastBlockHeight() {
		return this.lastBlock.getHeight();
	}

	/**
	 * Analyzes last block, used during initial initialization of blocks in blockchain.
	 *
	 * @param curBlock lastBlock in db.
	 */
	public void analyzeLastBlock(final DbBlock curBlock) {
		LOGGER.info("analyzing last block: " + Long.toString(curBlock.getShortId()));
		this.lastBlock = curBlock;
	}

	/**
	 * Adds new block into db
	 *
	 * @param block block to be added to db
	 * @return always true
	 */
	public boolean addBlockToDb(final Block block) {
		final DbBlock dbBlock = this.mapper.map(block);

		// hibernate will save both block AND transactions
		// as there is cascade in DbBlock
		// mind that there is NO cascade in transaction (near block field)
		this.blockDao.save(dbBlock);
		this.lastBlock = dbBlock;
		return true;
	}

	/**
	 * Removes block from the db after specified height
	 */
	public void dropDbBlocksAfter(final BlockHeight height) {
		this.blockDao.deleteBlocksAfterHeight(height);
		this.lastBlock = this.blockDao.findByHeight(height);
	}
}
