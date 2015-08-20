package org.nem.nis;

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
	private static final PrivateKey TEST_ADDRESS1_PK = PrivateKey.fromHexString("906ddbd7052149d7f45b73166f6b64c2d4f2fdfb886796371c0e32c03382bf33");
	private static final Address TEST_ADDRESS1 = Address.fromEncoded("TALICEQPBXSNJCZBCF7ZSLLXUBGUESKY5MZIA2IY");
	private static final Address TEST_ADDRESS2 = Address.fromEncoded("TBQGGC6ABX2SSYB33XXCSX3QS442YHJGYGWWSYYT");
	private static final PrivateKey TEST_BOOT_KEY = new KeyPair().getPrivateKey();

	private static final int AUTO_BOOT = 0x00000001;
	private static final int DELAY_BLOCK_LOADING = 0x00000002;
	private static final int HISTORICAL_ACCOUNT_DATA = 0x00000004;
	private static final int THROW_DURING_BOOT = 0x00000008;

	//region session auto-wiring

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

	//endregion

	//region cache

	@Test
	public void initUpdatesNisCacheWithNemesisBlockDataWhenNoBlocksArePresent() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		assertBlockAnalyzerUsed(context);
		Assert.assertThat(getBalance(context.nisCache, TEST_ADDRESS1), IsEqual.equalTo(Amount.fromNem(50_000_000L)));
		Assert.assertThat(getBalance(context.nisCache, TEST_ADDRESS2), IsEqual.equalTo(Amount.fromNem(50_000_000L)));
		context.assertNoErrors();
	}

	@Test
	public void initUpdatesNisCacheWhenMultipleBlocksArePresentUsingBlockAnalyzer() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Block block = NisUtils.createBlockList(context.nemesisBlock, 1).get(0);
		final Transaction transfer = new TransferTransaction(
				TimeInstant.ZERO,
				new Account(new KeyPair(TEST_ADDRESS1_PK)),
				new Account(TEST_ADDRESS2),
				Amount.fromNem(1_000_000),
				null);
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
		Assert.assertThat(getBalance(context.nisCache, TEST_ADDRESS1), IsEqual.equalTo(Amount.fromNem(48_999_900L)));
		Assert.assertThat(getBalance(context.nisCache, TEST_ADDRESS2), IsEqual.equalTo(Amount.fromNem(51_000_000L)));
		context.assertNoErrors();
	}

	private static void assertBlockAnalyzerUsed(final TestContext context) {
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).loadNemesisBlock();
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.any());
	}

	//endregion

	//region auto-boot

	@Test
	public void initBootsNetworkIfAutoBootIsEnabled() {
		// Arrange:
		final TestContext context = this.createTestContext(AUTO_BOOT);

		// Act:
		context.nisMain.init();

		// Assert:
		final ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
		Mockito.verify(context.networkHost, Mockito.only()).boot(nodeCaptor.capture());

		final NodeIdentity identity = nodeCaptor.getValue().getIdentity();
		Assert.assertThat(identity.getKeyPair().getPrivateKey(), IsEqual.equalTo(TEST_BOOT_KEY));
		Assert.assertThat(identity.getName(), IsEqual.equalTo("NisMain test"));

		final NodeEndpoint endpoint = nodeCaptor.getValue().getEndpoint();
		Assert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("ftp", "10.0.0.1", 100)));
		context.assertNoErrors();
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

	//endregion

	//region nemesis block saving

	@Test
	public void initSavesNemesisBlockIfDatabaseIsEmpty() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// sanity check
		Assert.assertThat(this.blockDao.findByHeight(BlockHeight.ONE), IsNull.nullValue());

		// Act:
		context.nisMain.init();
		final DbBlock dbBlock = this.blockDao.findByHeight(BlockHeight.ONE);

		// Assert:
		// - if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any());
		Assert.assertThat(dbBlock, IsNull.notNullValue());
		context.assertNoErrors();
	}

	@Test
	public void initDoesNotSaveNemesisBlockIfDatabaseIsNotEmpty() {
		// Arrange: add the nemesis block to the block dao
		final TestContext context = this.createTestContext();
		context.saveNemesisBlock();

		// sanity check
		Assert.assertThat(this.blockDao.findByHeight(BlockHeight.ONE), IsNull.notNullValue());

		// Act:
		context.nisMain.init();

		// Assert:
		// - if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any());
		context.assertNoErrors();
	}

	//endregion

	//region failures

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

	//endregion

	//region delay block loading

	@Test
	public void initLoadsDbAsynchronouslyIfDelayBlockLoadingIsEnabled() {
		// Arrange:
		final TestContext context = this.createTestContext(DELAY_BLOCK_LOADING);

		// Act:
		context.nisMain.init();

		// Assert:
		Assert.assertThat(context.blockChainLastBlockLayer.isLoading(), IsEqual.equalTo(true));
		context.assertNoErrors();
	}

	@Test
	public void initLoadsDbSynchronouslyIfDelayBlockLoadingIsDisabled() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		Assert.assertThat(context.blockChainLastBlockLayer.isLoading(), IsEqual.equalTo(false));
		context.assertNoErrors();
	}

	//endregion

	//region historical account data

	@Test
	public void initUsesNoHistoricalDataPruningIfHistoricalAccountDataIsEnabled() {
		// Arrange:
		final TestContext context = this.createTestContext(HISTORICAL_ACCOUNT_DATA);

		// Act:
		context.nisMain.init();

		// Assert:
		final EnumSet<ObserverOption> expectedOptions = EnumSet.of(ObserverOption.NoHistoricalDataPruning);
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.eq(expectedOptions));
		context.assertNoErrors();
	}

	@Test
	public void initUsesNoIncrementalPoiIfHistoricalAccountDataIsDisabled() {
		// Arrange:
		final TestContext context = this.createTestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		final EnumSet<ObserverOption> expectedOptions = EnumSet.of(ObserverOption.NoIncrementalPoi);
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.eq(expectedOptions));
		context.assertNoErrors();
	}

	//endregion

	private static Amount getBalance(final ReadOnlyNisCache cache, final Address address) {
		return cache.getAccountStateCache().findStateByAddress(address).getAccountInfo().getBalance();
	}

	private static NisConfiguration createNisConfiguration(
			final boolean autoBoot,
			final boolean delayBlockLoading,
			final boolean historicalAccountData) {
		final Properties defaultProperties = PropertiesExtensions.loadFromResource(CommonConfiguration.class, "config.properties", true);
		final Properties properties = new Properties();
		if (autoBoot) {
			properties.setProperty("nem.protocol", "ftp");
			properties.setProperty("nem.host", "10.0.0.1");
			properties.setProperty("nem.httpPort", "100");
			properties.setProperty("nis.bootKey", TEST_BOOT_KEY.toString());
			properties.setProperty("nis.bootName", "NisMain test");
		}

		if (!delayBlockLoading) {
			properties.setProperty("nis.delayBlockLoading", "false");
		}

		if (historicalAccountData) {
			properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
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
		private final Integer[] exitReason = new Integer[] { null };

		private TestContext(
				final BlockDao blockDao,
				final AccountDao accountDao) {
			this(blockDao, accountDao, 0);
		}

		private TestContext(
				final BlockDao blockDao,
				final AccountDao accountDao,
				final int flags) {
			this(
					blockDao,
					accountDao,
					0 != (flags & 0x01),
					0 != (flags & 0x02),
					0 != (flags & 0x04),
					0 != (flags & 0x08));
		}

		private TestContext(
				final BlockDao blockDao,
				final AccountDao accountDao,
				final boolean autoBoot,
				final boolean delayBlockLoading,
				final boolean historicalAccountData,
				final boolean throwDuringBoot) {
			this.blockDao = blockDao;
			this.accountDao = accountDao;
			this.mapper = Mockito.spy(MapperUtils.createModelToDbModelNisMapper(accountDao));

			final DefaultPoiFacade poiFacade = new DefaultPoiFacade(new MockImportanceCalculator());
			this.nisCache = NisCacheFactory.createReal(poiFacade);
			final BlockChainScoreManager scoreManager = new MockBlockChainScoreManager(this.nisCache.getAccountStateCache());
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			final NisMapperFactory nisMapperFactory = new NisMapperFactory(mapperFactory);
			this.blockChainLastBlockLayer = new BlockChainLastBlockLayer(blockDao, this.mapper);
			final BlockAnalyzer blockAnalyzer = new BlockAnalyzer(
					blockDao,
					scoreManager,
					this.blockChainLastBlockLayer,
					nisMapperFactory);
			this.nemesisBlock = blockAnalyzer.loadNemesisBlock();
			this.blockAnalyzer = Mockito.spy(blockAnalyzer);
			Mockito.when(this.networkHost.boot(Mockito.any())).thenAnswer(invocationOnMock -> {
				if (throwDuringBoot) {
					throw new Exception();
				}

				return CompletableFuture.completedFuture(null);
			});
			this.nisConfiguration = createNisConfiguration(autoBoot, delayBlockLoading, historicalAccountData);
			this.nisMain = new NisMain(
					blockDao,
					this.nisCache,
					this.networkHost,
					this.mapper,
					this.nisConfiguration,
					this.blockAnalyzer,
					i -> this.exitReason[0] = i);
		}

		public void saveBlock(final Block block) {
			final DbBlock dbBlock = MapperUtils.createModelToDbModelNisMapper(this.accountDao).map(block);
			this.blockDao.save(dbBlock);
		}

		public void saveNemesisBlock() {
			this.saveBlock(this.nemesisBlock);
		}

		public void assertNoErrors() {
			Assert.assertThat(this.exitReason[0], IsNull.nullValue());
		}

		public void assertError(final int expectedReason) {
			Assert.assertThat(this.exitReason[0], IsEqual.equalTo(expectedReason));
		}
	}
}