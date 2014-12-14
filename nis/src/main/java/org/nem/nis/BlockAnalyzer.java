package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.DeserializationContext;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.secret.*;
import org.nem.nis.service.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainScoreManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.logging.Logger;

// TODO 20141030: this class needs tests

/**
 * Loads and analyzes blocks from the database.
 */
public class BlockAnalyzer {
	private static final Logger LOGGER = Logger.getLogger(BlockAnalyzer.class.getName());

	private final BlockDao blockDao;
	private final BlockChainScoreManager blockChainScoreManager;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;

	/**
	 * Creates a new block analyzer.
	 *
	 * @param blockDao The block dao.
	 * @param blockChainScoreManager The block chain score manager.
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 */
	@Autowired(required = true)
	public BlockAnalyzer(
			final BlockDao blockDao,
			final BlockChainScoreManager blockChainScoreManager,
			final BlockChainLastBlockLayer blockChainLastBlockLayer) {
		this.blockDao = blockDao;
		this.blockChainScoreManager = blockChainScoreManager;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
	}

	/**
	 * Analyzes all blocks in the database.
	 *
	 * @param nisCache The cache.
	 * @return true if the analysis succeeded.
	 */
	public boolean analyze(final NisCache nisCache) {
		return this.analyze(nisCache, null);
	}

	/**
	 * Analyzes all blocks in the database up to the specified height.
	 *
	 * @param nisCache The cache.
	 * @param maxHeight The max height.
	 * @return true if the analysis succeeded.
	 */
	public boolean analyze(final NisCache nisCache, final Long maxHeight) {
		final Block nemesisBlock = this.loadNemesisBlock(nisCache);
		final Hash nemesisBlockHash = HashUtils.calculateHash(nemesisBlock);

		Long curBlockHeight;
		LOGGER.info("starting analysis...");

		org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(nemesisBlockHash);
		if (dbBlock == null) {
			LOGGER.severe("couldn't find nemesis block, did you remove OLD database?");
			return false;
		}

		LOGGER.info(String.format("first block generation hash: %s", dbBlock.getGenerationHash()));
		if (!dbBlock.getGenerationHash().equals(NemesisBlock.GENERATION_HASH)) {
			LOGGER.severe("couldn't find nemesis block, you're probably using developer's build, drop the db and rerun");
			return false;
		}

		Block parentBlock = null;
		final BlockIterator iterator = new BlockIterator(this.blockDao);

		// This is tricky:
		// we pass AA to observer and AutoCachedAA to toModel
		// it creates accounts for us inside AA but without height, so inside observer we'll set height
		final AccountCache accountCache = nisCache.getAccountCache();
		final BlockExecutor executor = new BlockExecutor(nisCache);
		final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(nisCache);
		do {
			final Block block = BlockMapper.toModel(dbBlock, accountCache.asAutoCache());

			if ((block.getHeight().getRaw() % 5000) == 0) {
				LOGGER.info(String.format("%d", block.getHeight().getRaw()));
			}

			if (null != parentBlock) {
				this.blockChainScoreManager.updateScore(parentBlock, block);
			}

			executor.execute(block, observer);

			// fully vest all transactions coming out of the nemesis block
			if (null == parentBlock) {
				for (final Account account : accountCache.contents()) {
					if (NemesisBlock.ADDRESS.equals(account.getAddress())) {
						continue;
					}

					final AccountState accountState = nisCache.getAccountStateCache().findStateByAddress(account.getAddress());
					accountState.getWeightedBalances().convertToFullyVested();
				}
			}

			parentBlock = block;

			curBlockHeight = dbBlock.getHeight() + 1;

			final org.nem.nis.dbmodel.Block currentBlock = iterator.findByHeight(curBlockHeight);
			if (currentBlock == null) {
				this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
			}

			dbBlock = currentBlock;

			if (null != maxHeight && dbBlock != null && dbBlock.getHeight() > maxHeight) {
				break;
			}
		} while (dbBlock != null);

		return true;
	}

	private static class BlockIterator {
		private final BlockDao blockDao;
		private long curHeight;
		private Iterator<org.nem.nis.dbmodel.Block> iterator;
		private boolean finished;

		public BlockIterator(final BlockDao blockDao) {
			this.curHeight = 1; // the nemesis block height
			this.blockDao = blockDao;
			this.finished = false;
		}

		public org.nem.nis.dbmodel.Block findByHeight(final long height) {
			if (this.finished) {
				return null;
			}

			if (null == this.iterator || !this.iterator.hasNext()) {
				this.iterator = this.blockDao.getBlocksAfter(new BlockHeight(this.curHeight), 2345).iterator();
				if (!this.iterator.hasNext()) {
					this.finished = true;
					return null;
				}
			}

			if (height != this.curHeight + 1) {
				throw new IllegalStateException("iterator was called for non-consecutive block");
			}

			final org.nem.nis.dbmodel.Block dbBlock = this.iterator.next();
			if (!dbBlock.getHeight().equals(height)) {
				throw new IllegalStateException("inconsistent db state, there's missing block you're probably using developer's build, drop the db and rerun");
			}

			this.curHeight = dbBlock.getHeight();
			return dbBlock;
		}
	}

	private NemesisBlock loadNemesisBlock(final NisCache nisCache) {
		// set up the nemesis block amounts
		nisCache.getAccountCache().addAccountToCache(NemesisBlock.ADDRESS);

		final AccountState nemesisState = nisCache.getAccountStateCache().findStateByAddress(NemesisBlock.ADDRESS);
		nemesisState.getAccountInfo().incrementBalance(NemesisBlock.AMOUNT);
		nemesisState.getWeightedBalances().addReceive(BlockHeight.ONE, NemesisBlock.AMOUNT);
		nemesisState.setHeight(BlockHeight.ONE);

		// load the nemesis block
		return NemesisBlock.fromResource(new DeserializationContext(nisCache.getAccountCache().asAutoCache()));
	}

	/**
	 * Loads the nemesis block.
	 *
	 * @return The nemesis block
	 */
	public NemesisBlock loadNemesisBlock() {
		return NemesisBlock.fromResource(new DeserializationContext(new DefaultAccountCache()));
	}
}
