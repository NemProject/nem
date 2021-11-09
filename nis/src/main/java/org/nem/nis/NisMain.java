package org.nem.nis;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.CommonStarter;
import org.nem.nis.boot.NetworkHostBootstrapper;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisModelToDbModelMapper;
import org.nem.nis.secret.ObserverOption;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

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
	private final NetworkHostBootstrapper networkHost;
	private final NisConfiguration nisConfiguration;
	private final NisModelToDbModelMapper mapper;
	private final BlockAnalyzer blockAnalyzer;
	private final Consumer<Integer> exitHandler;
	private final boolean[] exitHandlerCalled = new boolean[]{
			false
	};

	@Autowired(required = true)
	public NisMain(final BlockDao blockDao, final ReadOnlyNisCache nisCache, final NetworkHostBootstrapper networkHost,
			final NisModelToDbModelMapper mapper, final NisConfiguration nisConfiguration, final BlockAnalyzer blockAnalyzer,
			final Consumer<Integer> exitHandler) {
		this.blockDao = blockDao;
		this.nisCache = nisCache;
		this.networkHost = networkHost;
		this.mapper = mapper;
		this.nisConfiguration = nisConfiguration;
		this.blockAnalyzer = blockAnalyzer;
		this.exitHandler = i -> {
			if (this.exitHandlerCalled[0]) {
				return;
			}

			this.exitHandlerCalled[0] = true;
			exitHandler.accept(i);
		};
	}

	private void analyzeBlocks() {
		final NisCache nisCache = this.nisCache.copy();
		if (!this.blockAnalyzer.analyze(nisCache, this.buildOptions(this.nisConfiguration))) {
			this.exitHandler.accept(-1);
			throw new IllegalStateException("blockAnalyzer.analyze failed");
		}

		nisCache.commit();
	}

	@PostConstruct
	public void init() {
		LOGGER.warning("context ================== current: " + TIME_PROVIDER.getCurrentTime());

		// load the nemesis block information (but do not update the cache)
		this.nemesisBlock = this.blockAnalyzer.loadNemesisBlock();
		this.nemesisBlockHash = HashUtils.calculateHash(this.nemesisBlock);
		this.logNemesisInformation();

		// initialize the database
		this.populateDb();

		// analyze the blocks
		final CompletableFuture<?> future = CompletableFuture.runAsync(this::analyzeBlocks).thenCompose(v1 -> {
			final boolean shouldAutoBoot = this.nisConfiguration.shouldAutoBoot();
			PrivateKey autoBootKey = this.nisConfiguration.getAutoBootKey();
			String autoBootName = this.nisConfiguration.getAutoBootName();
			if (null == autoBootKey) {
				if (!shouldAutoBoot) {
					LOGGER.info("auto-boot is off");
					return CompletableFuture.completedFuture(null);
				}

				autoBootKey = new KeyPair().getPrivateKey();
			}

			if (null == autoBootName) {
				final KeyPair keyPair = new KeyPair(autoBootKey);
				autoBootName = Address.fromPublicKey(keyPair.getPublicKey()).toString();
			}

			final NodeIdentity autoBootNodeIdentity = new NodeIdentity(new KeyPair(autoBootKey), autoBootName);
			LOGGER.warning(String.format("auto-booting %s ... ", autoBootNodeIdentity.getAddress()));
			return this.networkHost.boot(new Node(autoBootNodeIdentity, this.nisConfiguration.getEndpoint()))
					.thenAccept(v2 -> LOGGER.warning("auto-booted!"));
		}).exceptionally(e -> {
			LOGGER.severe("something really bad happened: " + e);
			this.exitHandler.accept(-2);
			return null;
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

		final DbBlock dbBlock = this.mapper.map(this.nemesisBlock);
		this.blockDao.save(dbBlock);
	}

	private EnumSet<ObserverOption> buildOptions(final NisConfiguration config) {
		// when HISTORICAL_ACCOUNT_DATA is enabled, use incremental POI when loading to make sure that
		// all the historical importances are loaded into memory
		final EnumSet<ObserverOption> options = EnumSet.noneOf(ObserverOption.class);
		if (config.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)) {
			options.add(ObserverOption.NoHistoricalDataPruning);
		} else {
			options.add(ObserverOption.NoIncrementalPoi);
		}

		final BlockChainConfiguration blockChainConfiguration = config.getBlockChainConfiguration();
		if (blockChainConfiguration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE)) {
			options.add(ObserverOption.NoOutlinkObserver);
			options.remove(ObserverOption.NoIncrementalPoi);
		}

		return options;
	}
}
