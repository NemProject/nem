package org.nem.nis;

import org.hibernate.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.nis.boot.NetworkHostBootstrapper;
import org.nem.nis.cache.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.ImportanceCalculator;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.nis.test.*;
import org.nem.specific.deploy.NisConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@ContextConfiguration(classes = TestConf.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NisMainTest {

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
	public void initBootsNetworkIfAutoBootIsEnabled() {
		// Arrange:
		final TestContext context = new TestContext(true, false, false);

		// Act:
		context.nisMain.init();

		// Assert:
		Mockito.verify(context.networkHost, Mockito.only()).boot(Mockito.any());
	}

	@Test
	public void initDoesNotSaveNemesisBlockIfDatabaseIsNotEmpty() {
		// Arrange:
		final TestContext context = new TestContext(false, false, false);
		final Block block = context.blockAnalyzer.loadNemesisBlock();
		final DbBlock dbBlock = MapperUtils.createModelToDbModelNisMapper(accountDao).map(block);
		this.blockDao.save(dbBlock);

		// Act:
		context.nisMain.init();

		// Assert:
		// if nemesis block would have been saved during init, it would have been mapped to a dbBlock.
		Mockito.verify(context.mapper, Mockito.never()).map(block);
	}

	@Test
	public void initFailsIfDatabaseContainsInvalidNemesisBlock() {
		// Arrange:
		final TestContext context = new TestContext(true, false, false);
		final Block block = NisUtils.createRandomBlock();
		block.sign();
		final DbBlock dbBlock = context.mapper.map(block);
		this.blockDao.save(dbBlock);

		// Act:
		context.nisMain.init();

		// Assert:
		// TODO 20150819 BR -> J: when BlockAnalyzer.analyzeBlocks() returns false, the test immediately exits.
		// > not sure how to test that.
	}

	private static class MockImportanceCalculator implements ImportanceCalculator {
		@Override
		public void recalculate(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
			accountStates.stream().forEach(a -> a.getImportanceInfo().setImportance(blockHeight, 1.0 / accountStates.size()));
		}
	}

	private class MockBlockChainScoreManager implements BlockChainScoreManager {
		private final ReadOnlyAccountStateCache accountStateCache;
		private BlockChainScore score = BlockChainScore.ZERO;

		private MockBlockChainScoreManager(final ReadOnlyAccountStateCache accountStateCache) {
			this.accountStateCache = accountStateCache;
		}

		@Override
		public BlockChainScore getScore() {
			return this.score;
		}

		@Override
		public void updateScore(final Block parentBlock, final Block block) {
			final BlockScorer scorer = new BlockScorer(this.accountStateCache);
			this.score = this.score.add(new BlockChainScore(scorer.calculateBlockScore(parentBlock, block)));
		}
	}

	private static Properties getCommonProperties() {
		final Properties properties = new Properties();
		properties.setProperty("nem.shortServerName", "Nis");
		properties.setProperty("nem.folder", "folder");
		properties.setProperty("nem.maxThreads", "1");
		properties.setProperty("nem.protocol", "ftp");
		properties.setProperty("nem.host", "10.0.0.1");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");
		properties.setProperty("nem.webContext", "/web");
		properties.setProperty("nem.apiContext", "/api");
		properties.setProperty("nem.homePath", "/home");
		properties.setProperty("nem.shutdownPath", "/shutdown");
		properties.setProperty("nem.useDosFilter", "true");
		return properties;
	}

	private static NisConfiguration createNisConfiguration(
			final boolean autoBoot,
			final boolean delayBlockLoading,
			final boolean historicalAccountData) {
		final Properties properties = getCommonProperties();
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
		private final BlockAnalyzer blockAnalyzer;
		private final NetworkHostBootstrapper networkHost = Mockito.mock(NetworkHostBootstrapper.class);
		private final NisConfiguration nisConfiguration;
		private final NisMain nisMain;

		private TestContext() {
			this(false, false, false);
		}

		private TestContext(
				final boolean autoBoot,
				final boolean delayBlockLoading,
				final boolean historicalAccountData) {
			final DefaultPoiFacade poiFacade = new DefaultPoiFacade(new MockImportanceCalculator());
			this.nisCache = NisCacheFactory.createReal(poiFacade);
			final BlockChainScoreManager scoreManager = new MockBlockChainScoreManager(this.nisCache.getAccountStateCache());
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			final NisMapperFactory nisMapperFactory = new NisMapperFactory(mapperFactory);
			final BlockChainLastBlockLayer blockChainLastBlockLayer = new BlockChainLastBlockLayer(blockDao, this.mapper);
			this.blockAnalyzer = Mockito.spy(new BlockAnalyzer(
					blockDao,
					scoreManager,
					blockChainLastBlockLayer,
					nisMapperFactory));
			Mockito.when(this.networkHost.boot(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
			this.nisConfiguration = Mockito.spy(createNisConfiguration(autoBoot, delayBlockLoading, historicalAccountData));
			this.nisMain = new NisMain(
					blockDao,
					this.nisCache,
					this.networkHost,
					this.mapper,
					this.nisConfiguration,
					this.blockAnalyzer);
		}
	}



	// TODO 20150819 BR -> *: this looks like outdated stuff. Do we still need it?
	private static final List<String> PRIVATE_KEY_STRINGS = Arrays.asList(
			"983bb01d05edecfaef55df9486c111abb6299c754a002069b1d0ef4537441bda",
			"c2dd81157ecbe0bda5d0a2d38e826887da201b05d5fa0b6b241186f731b37674",
			"053ba4b2b2204668668f371f1c76276b849a1e29402660d6c82631423f3560dd",
			"1f8d73a13abaf266bb819c390e26bff0b3ea46b5366798f7489d20ae7149bd7b",
			"71655699ce872415bed291cc05e8f06b29a6b14a1da18dc1ea84d092a401e572",
			"823541e7e0a9e61387bcc66dabf3e0b9257ca168437a01907f82c6012ecc896f",
			"c0f885f4e70dd86cf10d926fee80d2bb051e00bafaa1b2d54771d8b1096498fb",
			"3029c55412442244defb01deef360db9b6ddf4779479e1436e67028dc44ca5f7",
			"1e2fcb717b7f10b631224c949529e878f4188a961c9d10ed3863eda93b77f5a3",
			"fb1a7d3399ef4722fb04b017cef8762fcc42a43eee987b55bb3a3948ea7cb44e",
			"e8da26bf835b3caca4712b8ca7cf893dce6e1cd1e00fe8601a392fea043f69df");

	private static final List<String> ENCODED_ADDRESS_STRINGS = Arrays.asList(
			"TALICELCD3XPH4FFI5STGGNSNSWPOTG5E4DS2TOS",
			"TALICEW2K5Q6O5MQ3UK4TEW4ND7QSA4PFIBEXDK4",
			"TALICERWZAJZ33IDFCLS7H44ULQTDNMG5KU7Y4UL",
			"TALICE4AQNH5TE7O43RZ5FPJ3AC6HCFTSOO7B3GF",
			"TATHIESLV5OI35KOLL3GODH2ZGSRUAI4GTS7IEBO",
			"TBMAKOTAFIG5P4EYBO7XLPNNSKRUCYQOZPDW27UA",
			"TDPATMAMYAICKQ7SPFFE3TRTHYW2XF773VTTHYUI",
			"TDGIMREMR5NSRFUOMPI5OOHLDATCABNPC5ID2SVA",
			"TD2T562S4H3XT3QADUYYWEJ4EKJAGUARHGGOLHQQ",
			"TCLOITQWQ4KWWA6SEYUCG6VIVTXNH35LBCFYV4GN",
			"TCKRYSTAID2VC2ZW3MPM2FHKIFV2YZUJVMYPPP24");

	private static final List<KeyPair> KEY_PAIRS = PRIVATE_KEY_STRINGS.stream()
			.map(s -> new KeyPair(PrivateKey.fromHexString(s)))
			.collect(Collectors.toList());

	@Test
	public void printOutPrivateKeys() {
		System.out.println("*** private keys ***");
		printOutPairs(keyPair -> (String)JsonSerializer.serializeToJson(keyPair.getPrivateKey()).get("value"));
	}

	@Test
	public void printOutPublicKeys() {
		System.out.println("*** public keys ***");
		printOutPairs(keyPair -> (String)JsonSerializer.serializeToJson(keyPair.getPublicKey()).get("value"));
	}

	private static void printOutPairs(final Function<KeyPair, String> toString) {
		for (final KeyPair keyPair : KEY_PAIRS) {
			final Address address = getAddress(keyPair);
			final String serializedKey = toString.apply(keyPair);
			System.out.println(String.format("'%s': '%s',", address.getEncoded(), serializedKey));
		}
	}

	private static Address getAddress(final KeyPair keyPair) {
		final Address address = Address.fromPublicKey(keyPair.getPublicKey());

		for (final String encodedAddress : ENCODED_ADDRESS_STRINGS) {
			if (encodedAddress.equalsIgnoreCase(address.getEncoded())) {
				return Address.fromEncoded(encodedAddress);
			}
		}

		throw new IllegalArgumentException(String.format("could not find %s", address));
	}
}