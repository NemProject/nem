package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.chain.BlockExecutor;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.nem.nis.secret.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainScoreManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.logging.Logger;

/**
 * Loads and analyzes blocks from the database.
 */
public class BlockAnalyzer {
	private static final Logger LOGGER = Logger.getLogger(BlockAnalyzer.class.getName());
	private static final int NUM_BLOCKS_TO_PULL_AT_ONCE = 100;

	private final BlockDao blockDao;
	private final BlockChainScoreManager blockChainScoreManager;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final NisMapperFactory mapperFactory;
	private final NemesisBlockInfo nemesisBlockInfo;
	private final int estimatedBlocksPerYear;

	/**
	 * Creates a new block analyzer.
	 *
	 * @param blockDao The block dao.
	 * @param blockChainScoreManager The block chain score manager.
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 * @param mapperFactory The mapper factory.
	 * @param estimatedBlocksPerYear The estimated number of blocks per year.
	 */
	@Autowired(required = true)
	public BlockAnalyzer(final BlockDao blockDao, final BlockChainScoreManager blockChainScoreManager,
			final BlockChainLastBlockLayer blockChainLastBlockLayer, final NisMapperFactory mapperFactory,
			final int estimatedBlocksPerYear) {
		this.blockDao = blockDao;
		this.blockChainScoreManager = blockChainScoreManager;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.mapperFactory = mapperFactory;
		this.nemesisBlockInfo = NetworkInfos.getDefault().getNemesisBlockInfo();
		this.estimatedBlocksPerYear = estimatedBlocksPerYear;
	}

	/**
	 * Analyzes all blocks in the database.
	 *
	 * @param nisCache The cache.
	 * @param options The observer options.
	 * @return true if the analysis succeeded.
	 */
	public boolean analyze(final NisCache nisCache, final EnumSet<ObserverOption> options) {
		return this.analyze(nisCache, options, null);
	}

	/**
	 * Analyzes all blocks in the database up to the specified height.
	 *
	 * @param nisCache The cache.
	 * @param maxHeight The max height.
	 * @return true if the analysis succeeded.
	 */
	public boolean analyze(final NisCache nisCache, final EnumSet<ObserverOption> options, final Long maxHeight) {
		final Block nemesisBlock = this.loadNemesisBlock(nisCache);
		final Hash nemesisBlockHash = HashUtils.calculateHash(nemesisBlock);

		Long curBlockHeight;
		LOGGER.info("starting analysis...");

		DbBlock dbBlock = this.blockDao.findByHeight(BlockHeight.ONE);
		if (!dbBlock.getBlockHash().equals(nemesisBlockHash)) {
			LOGGER.severe("couldn't find nemesis block, did you remove OLD database?");
			return false;
		}

		LOGGER.info(String.format("first block generation hash: %s", dbBlock.getGenerationHash()));
		if (!dbBlock.getGenerationHash().equals(this.nemesisBlockInfo.getGenerationHash())) {
			LOGGER.severe("couldn't find nemesis block, you're probably using developer's build, drop the db and rerun");
			return false;
		}

		Block parentBlock = null;
		final BlockIterator iterator = new BlockIterator(this.blockDao);

		final AccountCache accountCache = nisCache.getAccountCache();
		final BlockExecutor executor = new BlockExecutor(nisCache);
		final BlockTransactionObserver observer = new BlockTransactionObserverFactory(options, this.estimatedBlocksPerYear)
				.createExecuteCommitObserver(nisCache);
		final NisDbModelToModelMapper mapper = this.mapperFactory.createDbModelToModelNisMapper(accountCache);

		do {
			final Block block = mapper.map(dbBlock);

			if (null != parentBlock) {
				this.blockChainScoreManager.updateScore(parentBlock, block);
			}

			executor.execute(block, observer);

			parentBlock = block;

			curBlockHeight = dbBlock.getHeight() + 1;

			final DbBlock currentBlock = iterator.findByHeight(curBlockHeight);
			this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);

			dbBlock = currentBlock;

			if (null != maxHeight && dbBlock != null && dbBlock.getHeight() > maxHeight) {
				break;
			}
		} while (dbBlock != null);

		// note that curBlockHeight is one greater than the height of our last block
		this.recalculateImportancesAtHeight(nisCache, new BlockHeight(curBlockHeight - 1));
		this.blockChainLastBlockLayer.setLoaded();
		return true;
	}

	private void recalculateImportancesAtHeight(final NisCache nisCache, final BlockHeight height) {
		final BlockTransactionObserver recalculateObserver = new RecalculateImportancesObserver(nisCache);
		recalculateObserver.notify(new BalanceAdjustmentNotification(NotificationType.BlockHarvest,
				new Account(this.nemesisBlockInfo.getAddress()), Amount.ZERO),
				new BlockNotificationContext(height, TimeInstant.ZERO, NotificationTrigger.Execute));
	}

	private static class BlockIterator {
		private final BlockDao blockDao;
		private long curHeight;
		private Iterator<DbBlock> iterator;
		private boolean finished;

		public BlockIterator(final BlockDao blockDao) {
			this.curHeight = 1; // the nemesis block height
			this.blockDao = blockDao;
			this.finished = false;
		}

		public DbBlock findByHeight(final long height) {
			if (this.finished) {
				return null;
			}

			if (null == this.iterator || !this.iterator.hasNext()) {
				this.iterator = this.blockDao.getBlocksAfterAndUpdateCache(new BlockHeight(this.curHeight), NUM_BLOCKS_TO_PULL_AT_ONCE)
						.iterator();
				if (!this.iterator.hasNext()) {
					this.finished = true;
					return null;
				}
			}

			if (height != this.curHeight + 1) {
				throw new IllegalStateException("iterator was called for non-consecutive block");
			}

			final DbBlock dbBlock = this.iterator.next();
			if (!dbBlock.getHeight().equals(height)) {
				throw new IllegalStateException(
						"inconsistent db state, there's missing block you're probably using developer's build, drop the db and rerun");
			}

			this.curHeight = dbBlock.getHeight();
			return dbBlock;
		}
	}

	private Block loadNemesisBlock(final NisCache nisCache) {
		// set up the nemesis block amounts
		nisCache.getAccountCache().addAccountToCache(this.nemesisBlockInfo.getAddress());

		final AccountState nemesisState = nisCache.getAccountStateCache().findStateByAddress(this.nemesisBlockInfo.getAddress());
		nemesisState.getAccountInfo().incrementBalance(this.nemesisBlockInfo.getAmount());
		nemesisState.getWeightedBalances().addReceive(BlockHeight.ONE, this.nemesisBlockInfo.getAmount());
		nemesisState.setHeight(BlockHeight.ONE);

		// load the nemesis block
		return loadNemesisBlock(nisCache.getAccountCache());
	}

	/**
	 * Loads the nemesis block.
	 *
	 * @return The nemesis block
	 */
	public Block loadNemesisBlock() {
		return loadNemesisBlock(new DefaultAccountCache());
	}

	private static Block loadNemesisBlock(final AccountCache accountCache) {
		final NemesisBlockInfo nemesisBlockInfo = NetworkInfos.getDefault().getNemesisBlockInfo();
		return NemesisBlock.fromResource(nemesisBlockInfo, new DeserializationContext(accountCache));
	}
}
