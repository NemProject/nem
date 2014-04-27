package org.nem.nis;

import javax.annotation.PostConstruct;

import java.util.logging.Logger;

import org.nem.core.crypto.KeyPair;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;

import org.nem.nis.mappers.AccountDaoLookupAdapter;
import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.*;
import org.nem.core.time.*;
import org.springframework.beans.factory.annotation.Autowired;

public class NisMain {
	private static final Logger LOGGER = Logger.getLogger(NisMain.class.getName());

	public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

	private static Block GENESIS_BLOCK = new GenesisBlock(TIME_PROVIDER.getEpochTime());
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

	private void analyzeBlocks() {
		Long curBlockId;
		System.out.println("starting analysis...");

		org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHash(GENESIS_BLOCK_HASH);
		LOGGER.info("hex: " + HexEncoder.getString(dbBlock.getGenerationHash().getRaw()));
		if (null == dbBlock ||
				! dbBlock.getGenerationHash().equals(new Hash(HexEncoder.getBytes("c5d54f3ed495daec32b4cbba7a44555f9ba83ea068e5f1923e9edb774d207cd8")))) {
			LOGGER.severe("couldn't find genesis block, you're probably using developer's build, drop the db and rerun");
			System.exit(-1);
		}

		final Account genesisAccount = accountAnalyzer.addAccountToCache(GenesisBlock.ACCOUNT.getAddress());
		genesisAccount.incrementBalance(GenesisBlock.AMOUNT);

		do {
			final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer.asAutoCache());
			block.execute();

			curBlockId = dbBlock.getNextBlockId();
			if (null == curBlockId) {
				this.blockChain.analyzeLastBlock(dbBlock);
				break;
			}

			dbBlock = this.blockDao.findById(curBlockId);
			if (dbBlock == null && this.blockChain.getLastDbBlock() == null) {
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

		this.analyzeBlocks();

		this.networkHost.boot();
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
