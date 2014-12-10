package org.nem.nis;

import org.nem.core.crypto.*;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.dao.*;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.PoiAccountState;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

// TODO: we should really test this class ;)

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
	private final NisCache nisCache;
	private final NisPeerNetworkHost networkHost;
	private final NisConfiguration nisConfiguration;
	private final BlockAnalyzer blockAnalyzer;

	@Autowired(required = true)
	public NisMain(
			final AccountDao accountDao,
			final BlockDao blockDao,
			final NisCache nisCache,
			final NisPeerNetworkHost networkHost,
			final NisConfiguration nisConfiguration,
			final BlockAnalyzer blockAnalyzer) {
		this.accountDao = accountDao;
		this.blockDao = blockDao;
		this.nisCache = nisCache;
		this.networkHost = networkHost;
		this.nisConfiguration = nisConfiguration;
		this.blockAnalyzer = blockAnalyzer;
	}

	private void analyzeBlocks() {
		if (!this.blockAnalyzer.analyze(this.nisCache)) {
			System.exit(-1);
		}
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
		this.networkHost.boot(new Node(autoBootNodeIdentity, this.nisConfiguration.getEndpoint()));
		LOGGER.warning("auto-booted!");
	}

	private NemesisBlock loadNemesisBlock() {
		// set up the nemesis block amounts
		this.nisCache.getAccountCache().addAccountToCache(NemesisBlock.ADDRESS);

		final PoiAccountState nemesisState = this.nisCache.getPoiFacade().findStateByAddress(NemesisBlock.ADDRESS);
		nemesisState.getAccountInfo().incrementBalance(NemesisBlock.AMOUNT);
		nemesisState.getWeightedBalances().addReceive(BlockHeight.ONE, NemesisBlock.AMOUNT);
		nemesisState.setHeight(BlockHeight.ONE);

		// load the nemesis block
		return NemesisBlock.fromResource(new DeserializationContext(this.nisCache.getAccountCache().asAutoCache()));
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
		if (0 != this.blockDao.count()) {
			return;
		}

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
