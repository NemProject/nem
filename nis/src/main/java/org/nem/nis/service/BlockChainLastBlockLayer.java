package org.nem.nis.service;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
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
	private static final Logger LOGGER = Logger.getLogger(BlockChainLastBlockLayer.class.getName());

	private final BlockDao blockDao;
	private final NisModelToDbModelMapper mapper;

	private boolean isLoading;
	private DbBlock lastBlock;

	@Autowired(required = true)
	public BlockChainLastBlockLayer(final BlockDao blockDao, final NisModelToDbModelMapper mapper) {
		this.blockDao = blockDao;
		this.mapper = mapper;
		this.isLoading = true;
	}

	/**
	 * Gets the last block in the db.
	 *
	 * @return The last block in the db.
	 */
	public DbBlock getLastDbBlock() {
		return this.lastBlock;
	}

	/**
	 * Gets a value indicating whether or not blocks are being loaded.
	 *
	 * @return true if blocks are being loaded; false if all blocks have been loaded.
	 */
	public boolean isLoading() {
		return this.isLoading;
	}

	/**
	 * Sets a value indicating that all blocks have been loaded.
	 */
	public void setLoaded() {
		LOGGER.info(String.format("block loading completed; height %s", this.getLastBlockHeight()));
		this.isLoading = false;
	}

	/**
	 * Gets the height of the last analyzed block.
	 *
	 * @return The height of last analyzed block.
	 */
	public BlockHeight getLastBlockHeight() {
		return null == this.lastBlock ? BlockHeight.ONE : new BlockHeight(this.lastBlock.getHeight());
	}

	/**
	 * Analyzes last block, used during initial initialization of blocks in blockchain.
	 *
	 * @param curBlock The last block in the db.
	 */
	public void analyzeLastBlock(final DbBlock curBlock) {
		if (!this.isLoading()) {
			LOGGER.info(String.format("analyzing last block: %s @ %s", curBlock.getBlockHash(), curBlock.getHeight()));
		}

		this.lastBlock = curBlock;
	}

	/**
	 * Adds new block to the db.
	 *
	 * @param block The block to be added to the db.
	 */
	public void addBlockToDb(final Block block) {
		final DbBlock dbBlock = this.mapper.map(block);

		// hibernate will save both block AND transactions
		// as there is cascade in DbBlock
		// mind that there is NO cascade in transaction (near block field)
		this.blockDao.save(dbBlock);
		this.lastBlock = dbBlock;
	}

	/**
	 * Removes block from the db after specified height.
	 *
	 * @param height The height.
	 */
	public void dropDbBlocksAfter(final BlockHeight height) {
		// optimization: check if there is anything to delete
		if (0 < this.getLastBlockHeight().compareTo(height)) {
			this.blockDao.deleteBlocksAfterHeight(height);
			this.lastBlock = this.blockDao.findByHeight(height);
		}
	}
}
