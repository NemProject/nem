package org.nem.nis;

import org.nem.core.crypto.*;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisModelToDbModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
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

	private final BlockDao blockDao;
	private final ReadOnlyNisCache nisCache;
	private final NisPeerNetworkHost networkHost;
	private final NisConfiguration nisConfiguration;
	private final NisModelToDbModelMapper mapper;
	private final BlockAnalyzer blockAnalyzer;

	@Autowired(required = true)
	public NisMain(
			final BlockDao blockDao,
			final ReadOnlyNisCache nisCache,
			final NisPeerNetworkHost networkHost,
			final NisModelToDbModelMapper mapper,
			final NisConfiguration nisConfiguration,
			final BlockAnalyzer blockAnalyzer) {
		this.blockDao = blockDao;
		this.nisCache = nisCache;
		this.networkHost = networkHost;
		this.mapper = mapper;
		this.nisConfiguration = nisConfiguration;
		this.blockAnalyzer = blockAnalyzer;
	}

	private void analyzeBlocks() {
		final NisCache nisCache = this.nisCache.copy();
		if (!this.blockAnalyzer.analyze(nisCache)) {
			System.exit(-1);
		}

		nisCache.commit();
	}

	@PostConstruct
	private void init() {
		LOGGER.warning("context ================== current: " + TIME_PROVIDER.getCurrentTime());

		// load the nemesis block information (but do not update the cache)
		this.nemesisBlock = this.blockAnalyzer.loadNemesisBlock();
		this.nemesisBlockHash = HashUtils.calculateHash(this.nemesisBlock);
		this.logNemesisInformation();

		// initialize the database
		this.populateDb();

		// analyze the blocks
		final CompletableFuture<java.lang.Void> future = CompletableFuture.runAsync(() -> {
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
		});

		if (!this.nisConfiguration.delayBlockLoading()) {
			future.join();
		}
	}

	private void logNemesisInformation() {
		LOGGER.info("nemesis block hash:" + this.nemesisBlockHash);

		final Address nemesisAddress = this.nemesisBlock.getSigner().getAddress();
		LOGGER.info("nemesis account            public key: " + nemesisAddress.getPublicKey());
		LOGGER.info("nemesis account compressed public key: " + nemesisAddress.getEncoded());
		LOGGER.info("nemesis account generation hash      : " + this.nemesisBlock.getGenerationHash());
	}

	private void populateDb() {
		if (0 != this.blockDao.count()) {
			return;
		}

		this.saveBlock(this.nemesisBlock);
	}

	private DbBlock saveBlock(final Block block) {
		DbBlock dbBlock;

		dbBlock = this.blockDao.findByHash(this.nemesisBlockHash);
		if (null != dbBlock) {
			return dbBlock;
		}

		dbBlock = this.mapper.map(block);
		this.blockDao.save(dbBlock);
		return dbBlock;
	}
}
