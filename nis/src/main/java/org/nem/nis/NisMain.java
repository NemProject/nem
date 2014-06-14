package org.nem.nis;

import javax.annotation.PostConstruct;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.nem.core.crypto.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.*;
import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.*;
import org.nem.core.time.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.springframework.beans.factory.annotation.Autowired;

public class NisMain {
	private static final Logger LOGGER = Logger.getLogger(NisMain.class.getName());

	public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

	private static Block GENESIS_BLOCK = GenesisBlock.fromResource();
	private static Hash GENESIS_BLOCK_HASH = HashUtils.calculateHash(GENESIS_BLOCK);

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@Autowired
	private NisPeerNetworkHost networkHost;

	@Autowired
	private BlockChainLastBlockLayer blockChainLastBlockLayer;

	private void analyzeBlocks() {
		Long curBlockId;
		LOGGER.info("starting analysis...");

		org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHash(GENESIS_BLOCK_HASH);
		LOGGER.info(String.format("hex: %s", dbBlock.getGenerationHash()));
		if (!dbBlock.getGenerationHash().equals(Hash.fromHexString("c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8"))) {
			LOGGER.severe("couldn't find genesis block, you're probably using developer's build, drop the db and rerun");
			System.exit(-1);
		}

		Block parentBlock = null;
		final Account genesisAccount = this.accountAnalyzer.addAccountToCache(GenesisBlock.ADDRESS);
		genesisAccount.incrementBalance(GenesisBlock.AMOUNT);
		genesisAccount.getWeightedBalances().addFullyVested(GENESIS_BLOCK.getHeight(), GenesisBlock.AMOUNT);
		genesisAccount.setHeight(BlockHeight.ONE);

		// This is tricky:
		// we pass AA to observer and AutoCachedAA to toModel
		// it creates accounts for us inside AA but without height, so inside observer we'll set height
		final AccountsHeightObserver observer = new AccountsHeightObserver(this.accountAnalyzer);
		do {
			final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer.asAutoCache());

			if (null != parentBlock) {
				this.blockChain.updateScore(parentBlock, block);
			}

			block.subscribe(observer);
			block.execute();
			block.unsubscribe(observer);

			// fully vest all transactions coming out of the genesis block
			if (null == parentBlock) {
				for (final Account account : this.accountAnalyzer) {
					if (account.equals(genesisAccount)) {
						continue;
					}

					account.getWeightedBalances().convertToFullyVested();
				}
			}

			parentBlock = block;

			curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				this.blockChainLastBlockLayer.analyzeLastBlock(dbBlock);
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			if (dbBlock == null && this.blockChainLastBlockLayer.getLastDbBlock() == null) {
				LOGGER.severe("inconsistent db state, you're probably using developer's build, drop the db and rerun");
				System.exit(-1);
			}
		} while (dbBlock != null);

		LOGGER.info("Known accounts: " + this.accountAnalyzer.size());
	}

	@PostConstruct
	private void init() {
		LOGGER.warning("context ================== current: " + TIME_PROVIDER.getCurrentTime());

		logGenesisInformation();

		this.populateDb();

		// TODO: this is a temporary hack to run analyzeBlocks before syncing starts ...
		// TODO: really, loading the blocks from the db should be done in parallel with network discovery
		this.analyzeBlocks();

		final CompletableFuture networkHostBootFuture = this.networkHost.boot();
		final CompletableFuture allFutures = CompletableFuture.allOf(networkHostBootFuture);
		allFutures.join();
	}

	private static void logGenesisInformation() {
		LOGGER.info("genesis block hash:" + GENESIS_BLOCK_HASH);

		final KeyPair genesisKeyPair = GENESIS_BLOCK.getSigner().getKeyPair();
		final Address genesisAddress = GENESIS_BLOCK.getSigner().getAddress();
		LOGGER.info("genesis account private key          : " + genesisKeyPair.getPrivateKey());
		LOGGER.info("genesis account            public key: " + genesisKeyPair.getPublicKey());
		LOGGER.info("genesis account compressed public key: " + genesisAddress.getEncoded());
		LOGGER.info("genesis account generetion hash      : " + GENESIS_BLOCK.getGenerationHash());
	}

	private void populateDb() {
		if (0 != this.blockDao.count())
			return;

		this.saveBlock(GENESIS_BLOCK);
	}

	private org.nem.nis.dbmodel.Block saveBlock(final Block block) {
		org.nem.nis.dbmodel.Block dbBlock;

		dbBlock = this.blockDao.findByHash(GENESIS_BLOCK_HASH);
		if (null != dbBlock) {
			return dbBlock;
		}

		dbBlock = BlockMapper.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
		this.blockDao.save(dbBlock);
		return dbBlock;
	}
}
