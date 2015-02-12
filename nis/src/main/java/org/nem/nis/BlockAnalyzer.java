package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.nem.nis.secret.*;
import org.nem.nis.service.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainScoreManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
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
	private final NisMapperFactory mapperFactory;

	/**
	 * Creates a new block analyzer.
	 *
	 * @param blockDao The block dao.
	 * @param blockChainScoreManager The block chain score manager.
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 * @param mapperFactory The mapper factory.
	 */
	@Autowired(required = true)
	public BlockAnalyzer(
			final BlockDao blockDao,
			final BlockChainScoreManager blockChainScoreManager,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final NisMapperFactory mapperFactory) {
		this.blockDao = blockDao;
		this.blockChainScoreManager = blockChainScoreManager;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.mapperFactory = mapperFactory;
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

		DbBlock dbBlock = this.blockDao.findByHash(nemesisBlockHash);
		if (dbBlock == null) {
			LOGGER.severe("couldn't find nemesis block, did you remove OLD database?");
			return false;
		}

		this.blockChainLastBlockLayer.setCurrentBlock(dbBlock);

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
		final BlockTransactionObserver observer = new BlockTransactionObserverFactory()
				.createExecuteCommitObserver(nisCache, EnumSet.of(ObserverOption.NoIncrementalPoi));
		final NisDbModelToModelMapper mapper = this.mapperFactory.createDbModelToModelNisMapper(accountCache);

		do {
			final Block block = mapper.map(dbBlock);

			if ((block.getHeight().getRaw() % 512) == 0) {
				this.blockChainLastBlockLayer.setCurrentBlock(dbBlock);
			}
			if ((block.getHeight().getRaw() % 500) == 0) {
				LOGGER.info(String.format("%d", block.getHeight().getRaw()));
			}

			if (null != parentBlock) {
				this.blockChainScoreManager.updateScore(parentBlock, block);
			}

			executor.execute(block, observer);

			parentBlock = block;

			curBlockHeight = dbBlock.getHeight() + 1;

			final DbBlock currentBlock = iterator.findByHeight(curBlockHeight);
			if (currentBlock == null) {
				this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
			}

			dbBlock = currentBlock;

			if (null != maxHeight && dbBlock != null && dbBlock.getHeight() > maxHeight) {
				break;
			}
		} while (dbBlock != null);

		// note that curBlockHeight is one greater than the height of our last block
		recalculateImportancesAtHeight(nisCache, new BlockHeight(curBlockHeight - 1));
		return true;
	}

	private static void recalculateImportancesAtHeight(final NisCache nisCache, final BlockHeight height) {
		final BlockTransactionObserver recalculateObserver = new RecalculateImportancesObserver(nisCache);
		recalculateObserver.notify(
				new BalanceAdjustmentNotification(NotificationType.BlockHarvest, new Account(NemesisBlock.ADDRESS), Amount.ZERO),
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
				this.iterator = this.blockDao.getBlocksAfter(new BlockHeight(this.curHeight), 500).iterator();
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
		return NemesisBlock.fromResource(new DeserializationContext(nisCache.getAccountCache()));
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
