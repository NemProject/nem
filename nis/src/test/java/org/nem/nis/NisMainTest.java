package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.*;
import org.nem.core.node.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.deploy.*;
import org.nem.nis.boot.NetworkHostBootstrapper;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.nem.nis.secret.ObserverOption;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.nis.test.*;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NisMainTest {
	private static final PrivateKey TEST_ADDRESS1_PK = PrivateKey
			.fromHexString("906ddbd7052149d7f45b73166f6b64c2d4f2fdfb886796371c0e32c03382bf33");
	private static final Address TEST_ADDRESS1 = Address.fromEncoded("TALICEQPBXSNJCZBCF7ZSLLXUBGUESKY5MZIA2IY");
	private static final Address TEST_ADDRESS2 = Address.fromEncoded("TBQGGC6ABX2SSYB33XXCSX3QS442YHJGYGWWSYYT");
	private static final PrivateKey TEST_BOOT_KEY = new KeyPair().getPrivateKey();

	private static final int AUTO_BOOT = 0x00000001;
	private static final int SUPPLY_BOOT_KEY = 0x00000002;
	private static final int SUPPLY_BOOT_NAME = 0x00000004;
	private static final int DELAY_BLOCK_LOADING = 0x00000008;
	private static final int HISTORICAL_ACCOUNT_DATA = 0x000000010;
	private static final int PROOF_OF_STAKE = 0x000000020;
	private static final int THROW_DURING_BOOT = 0x00000040;

	// region session auto-wiring

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private MosaicIdCache mosaicIdCache;

	@Autowired
	private SessionFactory sessionFactory;

	private Session session;

	@Before
	public void before() {
		this.session = this.sessionFactory.openSession();
	}

	@After
	public void after() {
		DbTestUtils.dbCleanup(this.session);
		this.mosaicIdCache.clear();
		this.session.close();
	}

	// endregion

	// region cache

	@Test
	public void initUpdatesNisCacheWithNemesisBlockDataWhenNoBlocksArePresent() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		assertBlockAnalyzerUsed(context);
		MatcherAssert.assertThat(getBalance(context.nisCache, TEST_ADDRESS1), IsEqual.equalTo(Amount.fromNem(50_000_000L)));
		MatcherAssert.assertThat(getBalance(context.nisCache, TEST_ADDRESS2), IsEqual.equalTo(Amount.fromNem(50_000_000L)));
		context.assertNoErrors();
	}

	@Test
	public void initUpdatesNisCacheWhenMultipleBlocksArePresentUsingBlockAnalyzer() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Block block = NisUtils.createBlockList(context.nemesisBlock, 1).get(0);
		final Transaction transfer = new TransferTransaction(TimeInstant.ZERO, new Account(new KeyPair(TEST_ADDRESS1_PK)),
				new Account(TEST_ADDRESS2), Amount.fromNem(1_000_000), null);
		transfer.setFee(Amount.fromNem(100));
		transfer.sign();
		block.addTransaction(transfer);
		block.sign();
		context.saveNemesisBlock();
		context.saveBlock(block);

		// Act:
		context.nisMain.init();

		// Assert:
		assertBlockAnalyzerUsed(context);
		MatcherAssert.assertThat(getBalance(context.nisCache, TEST_ADDRESS1), IsEqual.equalTo(Amount.fromNem(48_999_900L)));
		MatcherAssert.assertThat(getBalance(context.nisCache, TEST_ADDRESS2), IsEqual.equalTo(Amount.fromNem(51_000_000L)));
		context.assertNoErrors();
	}

	private static void assertBlockAnalyzerUsed(final TestContext context) {
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).loadNemesisBlock();
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.any());
	}

	// endregion

	// region auto-boot

	private void assertBootConfiguration(final int flags, final PrivateKey bootKey, final String bootName) {
		// Arrange:
		final TestContext context = this.createTestContext(flags);

		// Act:
		context.nisMain.init();

		// Assert:
		final ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
		Mockito.verify(context.networkHost, Mockito.only()).boot(nodeCaptor.capture());

		final NodeIdentity identity = nodeCaptor.getValue().getIdentity();
		if (0 != (flags & SUPPLY_BOOT_KEY)) {
			MatcherAssert.assertThat(identity.getKeyPair().getPrivateKey(), IsEqual.equalTo(bootKey));
		}

		if (0 != (flags & SUPPLY_BOOT_NAME)) {
			MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo(bootName));
		} else {
			MatcherAssert.assertThat(identity.getName(), IsEqual.equalTo(identity.getAddress().toString()));
		}

		final NodeEndpoint endpoint = nodeCaptor.getValue().getEndpoint();
		MatcherAssert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("ftp", "10.0.0.1", 100)));
		context.assertNoErrors();
	}

	@Test
	public void initBootsNetworkIfOnlyBootKeyIsAvailable() {
		// Assert:
		this.assertBootConfiguration(SUPPLY_BOOT_KEY, TEST_BOOT_KEY, null);
	}

	@Test
	public void initBootsNetworkIfAutoBootIsSet() {
		// Assert:
		this.assertBootConfiguration(AUTO_BOOT, null, null);
	}

	@Test
	public void autoBootRespectsBootKey() {
		// Assert:
		this.assertBootConfiguration(AUTO_BOOT | SUPPLY_BOOT_KEY, TEST_BOOT_KEY, null);
	}

	@Test
	public void autoBootRespectsBootName() {
		// Assert:
		this.assertBootConfiguration(AUTO_BOOT | SUPPLY_BOOT_NAME, null, "NisMain test");
	}

	@Test
	public void initDoesNotBootNetworkIfAutoBootIsDisabled() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
		context.assertNoErrors();
	}

	// endregion

	// region nemesis block saving

	@Test
	public void initSavesNemesisBlockIfDatabaseIsEmpty() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// sanity check
		MatcherAssert.assertThat(this.blockDao.findByHeight(BlockHeight.ONE), IsNull.nullValue());

		// Act:
		context.nisMain.init();
		final DbBlock dbBlock = this.blockDao.findByHeight(BlockHeight.ONE);

		// Assert:
		// - if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any());
		MatcherAssert.assertThat(dbBlock, IsNull.notNullValue());
		context.assertNoErrors();
	}

	@Test
	public void initDoesNotSaveNemesisBlockIfDatabaseIsNotEmpty() {
		// Arrange: add the nemesis block to the block dao
		final TestContext context = this.createTestContext();
		context.saveNemesisBlock();

		// sanity check
		MatcherAssert.assertThat(this.blockDao.findByHeight(BlockHeight.ONE), IsNull.notNullValue());

		// Act:
		context.nisMain.init();

		// Assert:
		// - if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any());
		context.assertNoErrors();
	}

	// endregion

	// region failures

	@Test
	public void initFailsIfDatabaseContainsNemesisBlockWithWrongBlockHash() {
		// Arrange:
		final TestContext context = this.createTestContext(AUTO_BOOT);
		final Block block = NisUtils.createRandomBlock();
		block.sign();
		context.saveBlock(block);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
		context.assertError(-1);
	}

	@Test
	public void initFailsIfDatabaseContainsNemesisBlockWithWrongGenerationHash() {
		// Arrange:
		final TestContext context = this.createTestContext(AUTO_BOOT);
		final Block block = context.blockAnalyzer.loadNemesisBlock();
		block.setPreviousGenerationHash(Utils.generateRandomHash());
		context.saveBlock(block);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
		context.assertError(-1);
	}

	@Test
	public void initFailsIfExceptionIsThrownDuringBoot() {
		// Arrange:
		final TestContext context = this.createTestContext(AUTO_BOOT | THROW_DURING_BOOT);
		context.saveNemesisBlock();

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.only()).boot(Mockito.any());
		context.assertError(-2);
	}

	// endregion

	// region delay block loading

	@Test
	public void initLoadsDbAsynchronouslyIfDelayBlockLoadingIsEnabled() {
		// Arrange:
		final TestContext context = this.createTestContext(DELAY_BLOCK_LOADING);

		// Act:
		context.nisMain.init();

		// Assert:
		MatcherAssert.assertThat(context.blockChainLastBlockLayer.isLoading(), IsEqual.equalTo(true));
		context.assertNoErrors();
	}

	@Test
	public void initLoadsDbSynchronouslyIfDelayBlockLoadingIsDisabled() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		MatcherAssert.assertThat(context.blockChainLastBlockLayer.isLoading(), IsEqual.equalTo(false));
		context.assertNoErrors();
	}

	// endregion

	// region feature -> observer mapping

	@Test
	public void initUsesNoHistoricalDataPruningIfHistoricalAccountDataIsEnabled() {
		// Assert:
		this.assertFlagsToOptionsMapping(HISTORICAL_ACCOUNT_DATA, EnumSet.of(ObserverOption.NoHistoricalDataPruning));
	}

	@Test
	public void initUsesNoOutlinkObserverAndIncrementalPoiIfProofOfStateIsEnabled() {
		// Assert:
		this.assertFlagsToOptionsMapping(PROOF_OF_STAKE, EnumSet.of(ObserverOption.NoOutlinkObserver));
	}

	@Test
	public void initSupportsNoHistoricalDataPruningForProofOfStake() {
		// Assert:
		this.assertFlagsToOptionsMapping(HISTORICAL_ACCOUNT_DATA | PROOF_OF_STAKE,
				EnumSet.of(ObserverOption.NoHistoricalDataPruning, ObserverOption.NoOutlinkObserver));
	}

	@Test
	public void initUsesDefaultOptionsIfNoFeaturesAreSelected() {
		// Assert:
		this.assertFlagsToOptionsMapping(0, EnumSet.of(ObserverOption.NoIncrementalPoi));
	}

	private void assertFlagsToOptionsMapping(final int flags, final EnumSet<ObserverOption> expectedOptions) {
		// Arrange:
		final TestContext context = this.createTestContext(flags);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.eq(expectedOptions));
		context.assertNoErrors();
	}

	// endregion

	private static Amount getBalance(final ReadOnlyNisCache cache, final Address address) {
		return cache.getAccountStateCache().findStateByAddress(address).getAccountInfo().getBalance();
	}

	private static NisConfiguration createNisConfiguration(final boolean autoBoot, final boolean supplyBootKey,
			final boolean supplyBootName, final boolean delayBlockLoading, final boolean historicalAccountData,
			final boolean proofOfStake) {
		final Properties defaultProperties = PropertiesExtensions.loadFromResource(CommonConfiguration.class, "config-default.properties",
				true);
		final Properties properties = new Properties();
		properties.setProperty("nem.protocol", "ftp");
		properties.setProperty("nem.host", "10.0.0.1");
		properties.setProperty("nem.httpPort", "100");
		if (!autoBoot) {
			properties.setProperty("nis.shouldAutoBoot", "false");
		}

		if (supplyBootKey) {
			properties.setProperty("nis.bootKey", TEST_BOOT_KEY.toString());
		}

		if (supplyBootName) {
			properties.setProperty("nis.bootName", "NisMain test");
		}

		if (!delayBlockLoading) {
			properties.setProperty("nis.delayBlockLoading", "false");
		}

		if (historicalAccountData) {
			properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		}

		if (proofOfStake) {
			properties.setProperty("nis.blockChainFeatures", "PROOF_OF_STAKE");
		}

		return new NisConfiguration(PropertiesExtensions.merge(Arrays.asList(defaultProperties, properties)));
	}

	private TestContext createTestContext() {
		return new TestContext(this.blockDao, this.accountDao);
	}

	private TestContext createTestContext(final int flags) {
		return new TestContext(this.blockDao, this.accountDao, flags);
	}

	private static class TestContext {
		private static final int ESTIMATED_BLOCKS_PER_YEAR = 1234;
		private final BlockDao blockDao;
		private final AccountDao accountDao;
		private final NisModelToDbModelMapper mapper;
		private final ReadOnlyNisCache nisCache;
		private final BlockChainLastBlockLayer blockChainLastBlockLayer;
		private final Block nemesisBlock;
		private final BlockAnalyzer blockAnalyzer;
		private final NetworkHostBootstrapper networkHost = Mockito.mock(NetworkHostBootstrapper.class);
		private final NisConfiguration nisConfiguration;
		private final NisMain nisMain;
		private final Integer[] exitReason = new Integer[]{
				null
		};

		private TestContext(final BlockDao blockDao, final AccountDao accountDao) {
			this(blockDao, accountDao, 0);
		}

		private TestContext(final BlockDao blockDao, final AccountDao accountDao, final int flags) {
			this(blockDao, accountDao, 0 != (flags & AUTO_BOOT), 0 != (flags & SUPPLY_BOOT_KEY), 0 != (flags & SUPPLY_BOOT_NAME),
					0 != (flags & DELAY_BLOCK_LOADING), 0 != (flags & HISTORICAL_ACCOUNT_DATA), 0 != (flags & PROOF_OF_STAKE),
					0 != (flags & THROW_DURING_BOOT));
		}

		private TestContext(final BlockDao blockDao, final AccountDao accountDao, final boolean autoBoot, final boolean supplyBootKey,
				final boolean supplyBootName, final boolean delayBlockLoading, final boolean historicalAccountData,
				final boolean proofOfStake, final boolean throwDuringBoot) {
			this.blockDao = blockDao;
			this.accountDao = accountDao;
			this.mapper = Mockito.spy(MapperUtils.createModelToDbModelNisMapperAccountDao(accountDao));

			final DefaultPoxFacade poxFacade = new DefaultPoxFacade(new MockImportanceCalculator());
			this.nisCache = NisCacheFactory.createReal(poxFacade);
			final BlockChainScoreManager scoreManager = new MockBlockChainScoreManager(this.nisCache.getAccountStateCache());
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			final NisMapperFactory nisMapperFactory = new NisMapperFactory(mapperFactory);
			this.blockChainLastBlockLayer = new BlockChainLastBlockLayer(blockDao, this.mapper);
			final BlockAnalyzer blockAnalyzer = new BlockAnalyzer(blockDao, scoreManager, this.blockChainLastBlockLayer, nisMapperFactory,
					ESTIMATED_BLOCKS_PER_YEAR);
			this.nemesisBlock = blockAnalyzer.loadNemesisBlock();
			this.blockAnalyzer = Mockito.spy(blockAnalyzer);
			Mockito.when(this.networkHost.boot(Mockito.any())).thenAnswer(invocationOnMock -> {
				if (throwDuringBoot) {
					throw new Exception();
				}

				return CompletableFuture.completedFuture(null);
			});
			this.nisConfiguration = createNisConfiguration(autoBoot, supplyBootKey, supplyBootName, delayBlockLoading,
					historicalAccountData, proofOfStake);
			this.nisMain = new NisMain(blockDao, this.nisCache, this.networkHost, this.mapper, this.nisConfiguration, this.blockAnalyzer,
					i -> this.exitReason[0] = i);
		}

		public void saveBlock(final Block block) {
			final DbBlock dbBlock = MapperUtils.createModelToDbModelNisMapperAccountDao(this.accountDao).map(block);
			this.blockDao.save(dbBlock);
		}

		public void saveNemesisBlock() {
			this.saveBlock(this.nemesisBlock);
		}

		public void assertNoErrors() {
			MatcherAssert.assertThat(this.exitReason[0], IsNull.nullValue());
		}

		public void assertError(final int expectedReason) {
			MatcherAssert.assertThat(this.exitReason[0], IsEqual.equalTo(expectedReason));
		}
	}
}
