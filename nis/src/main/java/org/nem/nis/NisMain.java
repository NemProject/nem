package org.nem.nis;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.*;
import org.nem.nis.dao.*;
import org.nem.nis.mappers.*;
import org.nem.nis.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.logging.Logger;

public class NisMain {
	private static final Logger LOGGER = Logger.getLogger(NisMain.class.getName());

	/**
	 * The time provider.
	 */
	public static final TimeProvider TIME_PROVIDER = CommonStarter.TIME_PROVIDER;

	private Block nemesisBlock;
	private Hash nemesisBlockHash;

	private final AccountDao accountDao;
	private final BlockDao blockDao;
	private final AccountAnalyzer accountAnalyzer;
	private final BlockChain blockChain;
	private final NisPeerNetworkHost networkHost;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final NisConfiguration nisConfiguration;

	@Autowired(required = true)
	public NisMain(
			final AccountDao accountDao,
			final BlockDao blockDao,
			final AccountAnalyzer accountAnalyzer,
			final BlockChain blockChain,
			final NisPeerNetworkHost networkHost,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final NisConfiguration nisConfiguration) {
		this.accountDao = accountDao;
		this.blockDao = blockDao;
		this.accountAnalyzer = accountAnalyzer;
		this.blockChain = blockChain;
		this.networkHost = networkHost;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.nisConfiguration = nisConfiguration;
	}

	private void analyzeBlocks() {
		Long curBlockId;
		LOGGER.info("starting analysis...");

		org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(this.nemesisBlockHash);
		if (dbBlock == null) {
			LOGGER.severe("couldn't find nemesis block, did you remove OLD database?");
			System.exit(-1);
		}
		LOGGER.info(String.format("hex: %s", dbBlock.getGenerationHash()));
		if (!dbBlock.getGenerationHash().equals(Hash.fromHexString("c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8"))) {
			LOGGER.severe("couldn't find nemesis block, you're probably using developer's build, drop the db and rerun");
			System.exit(-1);
		}

		Block parentBlock = null;
		final BlockIterator iterator = new BlockIterator(this.blockDao);

		// TODO: we should really test this procedure ;)

		// This is tricky:
		// we pass AA to observer and AutoCachedAA to toModel
		// it creates accounts for us inside AA but without height, so inside observer we'll set height
		final AccountsHeightObserver observer = new AccountsHeightObserver(this.accountAnalyzer);
		do {
			final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer.asAutoCache());

			if ((block.getHeight().getRaw() % 5000) == 0) {
				LOGGER.warning(String.format("%d", block.getHeight().getRaw()));
			}

			if (null != parentBlock) {
				this.blockChain.updateScore(parentBlock, block);
			}

			new BlockExecutor().execute(block, Arrays.asList(observer));

			// fully vest all transactions coming out of the nemesis block
			if (null == parentBlock) {
				for (final Account account : this.accountAnalyzer) {
					if (NemesisBlock.ADDRESS.equals(account.getAddress())) {
						continue;
					}

					account.getWeightedBalances().convertToFullyVested();
				}
			}

			parentBlock = block;

			curBlockId = dbBlock.getNextBlockId();

			// This is proper exit from this loop
			if (null == curBlockId) {
				this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
				break;
			}

			dbBlock = iterator.findById(curBlockId);

			if (dbBlock == null && this.blockChainLastBlockLayer.getLastDbBlock() == null) {
				LOGGER.severe("inconsistent db state, you're probably using developer's build, drop the db and rerun");
				System.exit(-1);
			}
		} while (dbBlock != null);

		this.initializePoi(parentBlock.getHeight());
	}

	private static class BlockIterator {
		private final BlockDao blockDao;
		private long curHeight;
		private Iterator<org.nem.nis.dbmodel.Block> iterator;

		public BlockIterator(final BlockDao blockDao) {
			this.curHeight = 1; // the nemesis block height
			this.blockDao = blockDao;
		}

		public org.nem.nis.dbmodel.Block findById(final long id) {
			// ugly loop, this is equivalent to
			// dbBlock = this.blockDao.findById(curBlockId);
			org.nem.nis.dbmodel.Block dbBlock = null;
			do {
				if (null == this.iterator || !this.iterator.hasNext()) {
					this.iterator = this.blockDao.getBlocksAfter(curHeight, 2345).iterator();
				}

				// in most cases this won't make any loops
				while (this.iterator.hasNext()) {
					dbBlock = this.iterator.next();
					if (dbBlock.getId().equals(id)) {
						break;
					}

					if (dbBlock.getHeight().compareTo(this.curHeight + 1) > 0) {
						dbBlock = null;
					}
				}

				if (dbBlock == null) {
					break;
				}

				this.curHeight = dbBlock.getHeight();
			}
			while (!dbBlock.getId().equals(id));

			return dbBlock;
		}
	}

	private void initializePoi(final BlockHeight height) {
		LOGGER.info("Analyzed blocks: " + height);
		LOGGER.info("Known accounts: " + this.accountAnalyzer.size());
		LOGGER.info(String.format("Initializing PoI for (%d) accounts", this.accountAnalyzer.size()));
		final BlockScorer blockScorer = new BlockScorer(this.accountAnalyzer);
		final BlockHeight blockHeight = blockScorer.getGroupedHeight(height);
		this.accountAnalyzer.recalculateImportances(blockHeight);
		LOGGER.info("PoI initialized");
	}

	@PostConstruct
	private void init() {
		LOGGER.warning("context ================== current: " + TIME_PROVIDER.getCurrentTime());

		// load the nemesis block information
		this.nemesisBlock = this.loadNemesisBlock();
		this.nemesisBlockHash = HashUtils.calculateHash(this.nemesisBlock);
		this.logNemesisInformation();

		this.populateDb();

		this.analyzeBlocks();

		final PrivateKey autoBootKey = this.nisConfiguration.getAutoBootKey();
		final String autoBootName = this.nisConfiguration.getAutoBootName();
		if (null == autoBootKey) {
			LOGGER.info("auto-boot is off");
			return;
		}

		final NodeIdentity autoBootNodeIdentity = new NodeIdentity(new KeyPair(autoBootKey), autoBootName);
		LOGGER.warning(String.format("auto-booting %s ... ", autoBootNodeIdentity.getAddress()));
		this.networkHost.boot(new Node(autoBootNodeIdentity, NodeEndpoint.fromHost("127.0.0.1")));
		LOGGER.warning("auto-booted!");
	}

	private NemesisBlock loadNemesisBlock() {
		// set up the nemesis block amounts
		final Account nemesisAccount = this.accountAnalyzer.addAccountToCache(NemesisBlock.ADDRESS);
		nemesisAccount.incrementBalance(NemesisBlock.AMOUNT);
		nemesisAccount.getWeightedBalances().addReceive(BlockHeight.ONE, NemesisBlock.AMOUNT);
		nemesisAccount.setHeight(BlockHeight.ONE);

		// load the nemesis block
		return NemesisBlock.fromResource(new DeserializationContext(this.accountAnalyzer.asAutoCache()));
	}

	private void logNemesisInformation() {
		LOGGER.info("nemesis block hash:" + this.nemesisBlockHash);

		final KeyPair nemesisKeyPair = this.nemesisBlock.getSigner().getKeyPair();
		final Address nemesisAddress = this.nemesisBlock.getSigner().getAddress();
		LOGGER.info("nemesis account private key          : " + nemesisKeyPair.getPrivateKey());
		LOGGER.info("nemesis account            public key: " + nemesisKeyPair.getPublicKey());
		LOGGER.info("nemesis account compressed public key: " + nemesisAddress.getEncoded());
		LOGGER.info("nemesis account generation hash      : " + this.nemesisBlock.getGenerationHash());
	}

	private void populateDb() {
		if (0 != this.blockDao.count())
			return;

		this.saveBlock(this.nemesisBlock);
	}

	private org.nem.nis.dbmodel.Block saveBlock(final Block block) {
		org.nem.nis.dbmodel.Block dbBlock;

		dbBlock = this.blockDao.findByHash(this.nemesisBlockHash);
		if (null != dbBlock) {
			return dbBlock;
		}

		dbBlock = BlockMapper.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
		this.blockDao.save(dbBlock);
		return dbBlock;
	}
}
