package org.nem.nis;

import org.nem.core.crypto.*;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.boot.NetworkHostBootstrapper;
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
	private final NetworkHostBootstrapper networkHost;
	private final NisConfiguration nisConfiguration;
	private final NisModelToDbModelMapper mapper;
	private final BlockAnalyzer blockAnalyzer;

	@Autowired(required = true)
	public NisMain(
			final BlockDao blockDao,
			final ReadOnlyNisCache nisCache,
			final NetworkHostBootstrapper networkHost,
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
		final CompletableFuture<?> future = CompletableFuture.runAsync(this::analyzeBlocks)
				.thenCompose(v1 -> {
					final PrivateKey autoBootKey = this.nisConfiguration.getAutoBootKey();
					final String autoBootName = this.nisConfiguration.getAutoBootName();
					if (null == autoBootKey) {
						LOGGER.info("auto-boot is off");
						return CompletableFuture.completedFuture(null);
					}

					final NodeIdentity autoBootNodeIdentity = new NodeIdentity(new KeyPair(autoBootKey), autoBootName);
					LOGGER.warning(String.format("auto-booting %s ... ", autoBootNodeIdentity.getAddress()));
					return this.networkHost.boot(new Node(autoBootNodeIdentity, this.nisConfiguration.getEndpoint()))
							.thenAccept(v2 -> LOGGER.warning("auto-booted!"));
				})
				.exceptionally(e -> {
					LOGGER.severe("something really bad happened: " + e);
					System.exit(1);
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

		this.saveNemesisBlock(this.nemesisBlock);
	}

	private DbBlock saveNemesisBlock(final Block block) {
		DbBlock dbBlock;

		dbBlock = this.blockDao.findByHeight(BlockHeight.ONE);
		if (null != dbBlock) {
			if (!dbBlock.getBlockHash().equals(this.nemesisBlockHash)) {
				final String message = String.format(
						"block with height 1 is not nemesis block (expected '%s'; actual '%s')",
						this.nemesisBlockHash,
						dbBlock.getBlockHash());
				LOGGER.severe(message);
				throw new IllegalStateException(message);
			}

			return dbBlock;
		}

		dbBlock = this.mapper.map(block);
		this.blockDao.save(dbBlock);
		return dbBlock;
	}
}
