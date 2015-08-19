package org.nem.nis;

import org.hamcrest.core.*;
import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.boot.NetworkHostBootstrapper;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.nem.nis.secret.ObserverOption;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.nis.test.*;
import org.nem.specific.deploy.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NisMainTest {
	private static final Address TEST_ADDRESS = Address.fromEncoded("TALICEQPBXSNJCZBCF7ZSLLXUBGUESKY5MZIA2IY");
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

	//region basic operation

	@Test
	public void initDelegatesToMembers() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).loadNemesisBlock();
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.any());
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any());
		Mockito.verify(context.nisConfiguration, Mockito.times(1)).getAutoBootKey();
		Mockito.verify(context.nisConfiguration, Mockito.times(1)).getAutoBootName();
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
	}

	@Test
	public void initUpdatesNisCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		Assert.assertThat(getBalance(context.nisCache, TEST_ADDRESS), IsEqual.equalTo(Amount.fromNem(50_000_000L)));
	}

	//endregion

	//region auto-boot

	@Test
	public void initBootsNetworkIfAutoBootIsEnabled() {
		// Arrange:
		final TestContext context = new TestContext(AUTO_BOOT);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.only()).boot(Mockito.any());
	}

	@Test
	public void initDoesNotBootNetworkIfAutoBootIsDisabled() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
	}

	//endregion

	//region nemesis block saving

	@Test
	public void initSavesNemesisBlockIfDatabaseIsEmpty() {
		// Arrange:
		final TestContext context = new TestContext();

		// sanity check
		Assert.assertThat(this.blockDao.findByHeight(BlockHeight.ONE), IsNull.nullValue());

		// Act:
		context.nisMain.init();
		final DbBlock dbBlock = this.blockDao.findByHeight(BlockHeight.ONE);

		// Assert:
		// - if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.only()).map(Mockito.any());
		Assert.assertThat(dbBlock, IsNull.notNullValue());
	}

	@Test
	public void initDoesNotSaveNemesisBlockIfDatabaseIsNotEmpty() {
		// Arrange: add the nemesis block to the block dao
		final TestContext context = new TestContext();
		final Block block = context.blockAnalyzer.loadNemesisBlock();
		context.saveBlock(block);
		Mockito.reset(context.mapper);

		// sanity check
		Assert.assertThat(this.blockDao.findByHeight(BlockHeight.ONE), IsNull.notNullValue());

		// Act:
		context.nisMain.init();

		// Assert:
		// - if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any());
	}

	//endregion

	//region failures

	@Test
	public void initFailsIfDatabaseContainsNemesisBlockWithWrongBlockHash() {
		// Arrange:
		final TestContext context = new TestContext(AUTO_BOOT);
		final Block block = NisUtils.createRandomBlock();
		block.sign();
		context.saveBlock(block);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
		Assert.assertThat(context.exitReason, IsEqual.equalTo(-1));
	}

	@Test
	public void initFailsIfDatabaseContainsNemesisBlockWithWrongGenerationHash() {
		// Arrange:
		final TestContext context = new TestContext(AUTO_BOOT);
		final Block block = context.blockAnalyzer.loadNemesisBlock();
		block.setPreviousGenerationHash(Utils.generateRandomHash());
		context.saveBlock(block);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.never()).boot(Mockito.any());
		Assert.assertThat(context.exitReason, IsEqual.equalTo(-1));
	}

	@Test
	public void initFailsIfExceptionIsThrownDuringBoot() {
		// Arrange:
		final TestContext context = new TestContext(AUTO_BOOT | THROW_DURING_BOOT);
		final Block block = context.blockAnalyzer.loadNemesisBlock();
		context.saveBlock(block);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.only()).boot(Mockito.any());
		Assert.assertThat(context.exitReason, IsEqual.equalTo(-2));
	}

	//endregion

	//region delay block loading

	@Test
	public void initLoadsDbAsynchronouslyIfDelayBlockLoadingIsEnabled() {
		// Arrange:
		final TestContext context = new TestContext(DELAY_BLOCK_LOADING);

		// Act:
		context.nisMain.init();

		// Assert:
		Assert.assertThat(context.blockChainLastBlockLayer.isLoading(), IsEqual.equalTo(true));
	}

	@Test
	public void initLoadsDbSynchronouslyIfDelayBlockLoadingIsDisabled() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		Assert.assertThat(context.blockChainLastBlockLayer.isLoading(), IsEqual.equalTo(false));
	}

	//endregion

	//region historical account data

	@Test
	public void initUsesNoHistoricalDataPruningIfHistoricalAccountDataIsEnabled() {
		// Arrange:
		final TestContext context = new TestContext(HISTORICAL_ACCOUNT_DATA);

		// Act:
		context.nisMain.init();

		// Assert:
		final EnumSet<ObserverOption> expectedOptions = EnumSet.of(ObserverOption.NoHistoricalDataPruning);
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.eq(expectedOptions));
	}

	@Test
	public void initUsesNoIncrementalPoiIfHistoricalAccountDataIsDisabled() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.nisMain.init();

		// Assert:
		final EnumSet<ObserverOption> expectedOptions = EnumSet.of(ObserverOption.NoIncrementalPoi);
		Mockito.verify(context.blockAnalyzer, Mockito.times(1)).analyze(Mockito.any(), Mockito.eq(expectedOptions));
	}

	//endregion

	private static Amount getBalance(final ReadOnlyNisCache cache, final Address address) {
		return cache.getAccountStateCache().findStateByAddress(address).getAccountInfo().getBalance();
	}

	private static NisConfiguration createNisConfiguration(
			final boolean autoBoot,
			final boolean delayBlockLoading,
			final boolean historicalAccountData) {
		final Properties properties = DeployUtils.getCommonProperties();
		if (autoBoot) {
			final PrivateKey privateKey = new KeyPair().getPrivateKey();
			properties.setProperty("nis.bootKey", privateKey.toString());
			properties.setProperty("nis.bootName", "NisMain test");
		}

		if (!delayBlockLoading) {
			properties.setProperty("nis.delayBlockLoading", "false");
		}

		if (historicalAccountData) {
			properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		}

		return new NisConfiguration(properties);
	}

	private class TestContext {
		private final ReadOnlyNisCache nisCache;
		private final NisModelToDbModelMapper mapper = Mockito.spy(MapperUtils.createModelToDbModelNisMapper(accountDao));
		private final BlockChainLastBlockLayer blockChainLastBlockLayer;
		private final BlockAnalyzer blockAnalyzer;
		private final NetworkHostBootstrapper networkHost = Mockito.mock(NetworkHostBootstrapper.class);
		private final NisConfiguration nisConfiguration;
		private final NisMain nisMain;
		private Integer exitReason = 0;

		private TestContext() {
			this(0);
		}

		private TestContext(final int flags) {
			this(0 != (flags & 0x01), 0 != (flags & 0x02), 0 != (flags & 0x04), 0 != (flags & 0x08));
		}

		private TestContext(
				final boolean autoBoot,
				final boolean delayBlockLoading,
				final boolean historicalAccountData,
				final boolean throwDuringBoot) {
			final DefaultPoiFacade poiFacade = new DefaultPoiFacade(new MockImportanceCalculator());
			this.nisCache = NisCacheFactory.createReal(poiFacade);
			final BlockChainScoreManager scoreManager = new MockBlockChainScoreManager(this.nisCache.getAccountStateCache());
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			final NisMapperFactory nisMapperFactory = new NisMapperFactory(mapperFactory);
			this.blockChainLastBlockLayer = new BlockChainLastBlockLayer(blockDao, this.mapper);
			this.blockAnalyzer = Mockito.spy(new BlockAnalyzer(
					blockDao,
					scoreManager,
					blockChainLastBlockLayer,
					nisMapperFactory));
			Mockito.when(this.networkHost.boot(Mockito.any())).thenAnswer(invocationOnMock -> {
				if (throwDuringBoot) {
					throw new Exception();
				}

				return CompletableFuture.completedFuture(null);
			});
			this.nisConfiguration = Mockito.spy(createNisConfiguration(autoBoot, delayBlockLoading, historicalAccountData));
			this.nisMain = new NisMain(
					blockDao,
					this.nisCache,
					this.networkHost,
					this.mapper,
					this.nisConfiguration,
					this.blockAnalyzer,
					i -> this.exitReason = i);
		}

		public void saveBlock(final Block block) {
			final DbBlock dbBlock = this.mapper.map(block);
			NisMainTest.this.blockDao.save(dbBlock);
		}
	}
}