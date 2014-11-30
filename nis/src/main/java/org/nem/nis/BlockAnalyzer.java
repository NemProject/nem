package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.DeserializationContext;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.poi.*;
import org.nem.nis.secret.*;
import org.nem.nis.service.*;
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

	@Autowired(required = true)
	public BlockAnalyzer(
			final BlockDao blockDao,
			final BlockChainScoreManager blockChainScoreManager,
			final BlockChainLastBlockLayer blockChainLastBlockLayer) {
		this.blockDao = blockDao;
		this.blockChainScoreManager = blockChainScoreManager;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
	}

	public boolean analyze(final AccountAnalyzer accountAnalyzer, final HashCache transactionHashCache) {
		return this.analyze(accountAnalyzer, transactionHashCache, null);
	}

	public boolean analyze(final AccountAnalyzer accountAnalyzer, final HashCache transactionHashCache, final Long maxHeight) {
		final Block nemesisBlock = this.loadNemesisBlock(accountAnalyzer);
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
		final PoiFacade poiFacade = accountAnalyzer.getPoiFacade();
		final AccountCache accountCache = accountAnalyzer.getAccountCache();
		final BlockExecutor executor = new BlockExecutor(poiFacade, accountCache);
		final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(accountAnalyzer, transactionHashCache);
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
				for (final Account account : accountCache) {
					if (NemesisBlock.ADDRESS.equals(account.getAddress())) {
						continue;
					}

					final PoiAccountState accountState = poiFacade.findStateByAddress(account.getAddress());
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

		this.initializePoi(accountAnalyzer, parentBlock.getHeight());
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

	private void initializePoi(final AccountAnalyzer accountAnalyzer, final BlockHeight height) {
		LOGGER.info("Analyzed blocks: " + height);
		LOGGER.info("Known accounts: " + accountAnalyzer.getAccountCache().size());
		LOGGER.info(String.format("Initializing PoI for (%d) accounts", accountAnalyzer.getAccountCache().size()));
		final BlockHeight blockHeight = BlockScorer.getGroupedHeight(height);
		accountAnalyzer.getPoiFacade().recalculateImportances(blockHeight);
		LOGGER.info("PoI initialized");
	}

	private NemesisBlock loadNemesisBlock(final AccountAnalyzer accountAnalyzer) {
		// set up the nemesis block amounts
		final Account nemesisAccount = accountAnalyzer.getAccountCache().addAccountToCache(NemesisBlock.ADDRESS);
		nemesisAccount.incrementBalance(NemesisBlock.AMOUNT);

		final PoiAccountState nemesisState = accountAnalyzer.getPoiFacade().findStateByAddress(NemesisBlock.ADDRESS);
		nemesisState.getWeightedBalances().addReceive(BlockHeight.ONE, NemesisBlock.AMOUNT);
		nemesisState.setHeight(BlockHeight.ONE);

		// load the nemesis block
		return NemesisBlock.fromResource(new DeserializationContext(accountAnalyzer.getAccountCache().asAutoCache()));
	}

	// TODO 20141030: figure out if this should be integrated here or stay in main

	//private void logNemesisInformation() {
	//	LOGGER.info("nemesis block hash:" + this.nemesisBlockHash);
	//
	//	final KeyPair nemesisKeyPair = this.nemesisBlock.getSigner().getKeyPair();
	//	final Address nemesisAddress = this.nemesisBlock.getSigner().getAddress();
	//	LOGGER.info("nemesis account private key          : " + nemesisKeyPair.getPrivateKey());
	//	LOGGER.info("nemesis account            public key: " + nemesisKeyPair.getPublicKey());
	//	LOGGER.info("nemesis account compressed public key: " + nemesisAddress.getEncoded());
	//	LOGGER.info("nemesis account generation hash      : " + this.nemesisBlock.getGenerationHash());
	//}

	//private void populateDb() {
	//	if (0 != this.blockDao.count()) {
	//		return;
	//	}
	//
	//	this.saveBlock(this.nemesisBlock);
	//}
	//
	//private org.nem.nis.dbmodel.Block saveBlock(final Block block) {
	//	org.nem.nis.dbmodel.Block dbBlock;
	//
	//	dbBlock = this.blockDao.findByHash(this.nemesisBlockHash);
	//	if (null != dbBlock) {
	//		return dbBlock;
	//	}
	//
	//	dbBlock = BlockMapper.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
	//	this.blockDao.save(dbBlock);
	//	return dbBlock;
	//}
}
