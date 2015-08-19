package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.ImportanceCalculator;
import org.nem.nis.secret.ObserverOption;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.nis.test.*;

import java.util.*;

public class BlockAnalyzerTest {
	private static final EnumSet<ObserverOption> DEFAULT_OPTIONS = EnumSet.of(ObserverOption.NoIncrementalPoi);
	private static final Address TEST_ADDRESS1 = Address.fromEncoded("TBR55RKEBHRR33JJL7QUGJJ3T75EIHDDOWWLTNH3");
	private static final Address TEST_ADDRESS2 = Address.fromEncoded("TBQGGC6ABX2SSYB33XXCSX3QS442YHJGYGWWSYYT");

	@After
	public void resetNetwork() {
		NetworkInfos.setDefault(null);
	}

	//region loadNemesisBlock

	@Test
	public void loadNemesisBlockReturnsExpectedBlockForTestNetwork() {
		// Arrange:
		final TestContext context = new TestContext();
		final NemesisBlockInfo nemesisBlockInfo = NetworkInfos.getTestNetworkInfo().getNemesisBlockInfo();

		// Act:
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();

		// Assert:
		Assert.assertThat(nemesisBlock.getGenerationHash(), IsEqual.equalTo(nemesisBlockInfo.getGenerationHash()));
		Assert.assertThat(nemesisBlock.getSigner().getAddress(), IsEqual.equalTo(nemesisBlockInfo.getAddress()));
		Assert.assertThat(nemesisBlock.getTransactions().size(), IsEqual.equalTo(162));
	}

	@Test
	public void loadNemesisBlockReturnsExpectedBlockForMainNetwork() {
		// Arrange:
		setNetwork(NetworkInfos.getMainNetworkInfo());
		final TestContext context = new TestContext();
		final NemesisBlockInfo nemesisBlockInfo = NetworkInfos.getMainNetworkInfo().getNemesisBlockInfo();

		// Act:
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();

		// Assert:
		Assert.assertThat(nemesisBlock.getGenerationHash(), IsEqual.equalTo(nemesisBlockInfo.getGenerationHash()));
		Assert.assertThat(nemesisBlock.getSigner().getAddress(), IsEqual.equalTo(nemesisBlockInfo.getAddress()));
		Assert.assertThat(nemesisBlock.getTransactions().size(), IsEqual.equalTo(1353));
	}

	//endregion

	@Test
	public void analyzeWithNullMaxHeightLoadsCompleteDb() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 345);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(context.blockChainLastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(346)));
	}

	@Test
	public void analyzeWithMaxHeightLoadsBlocksUpToMaxHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 345);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS, 234L);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(context.blockChainLastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(234)));
	}

	@Test
	public void analyzeDelegatesToMembers() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 567);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(BlockHeight.ONE);
		Mockito.verify(context.blockDao, Mockito.times(6 + 1)).getBlocksAfterAndUpdateCache(Mockito.any(), Mockito.eq(100));
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(568)).analyzeLastBlock(Mockito.any());
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).setLoaded();
		Mockito.verify(context.scoreManager, Mockito.times(567)).updateScore(Mockito.any(), Mockito.any());
		Mockito.verify(context.nisMapperFactory, Mockito.only()).createDbModelToModelNisMapper(Mockito.any());
		Mockito.verify(context.importanceCalculator, Mockito.only()).recalculate(Mockito.any(), Mockito.any());
	}

	@Test
	public void analyzeWithNoHistoricalDataPruningRecalculatesImportancesEveryThreeHundredSixtyBlocks() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 750);
		final EnumSet<ObserverOption> options = EnumSet.of(ObserverOption.NoHistoricalDataPruning);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, options);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Mockito.verify(context.importanceCalculator, Mockito.times(3)).recalculate(Mockito.any(), Mockito.any());
	}

	@Test
	public void analyzeSetsUpNemesisAccountInformationInNisCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final NemesisBlockInfo nemesisBlockInfo = NetworkInfos.getTestNetworkInfo().getNemesisBlockInfo();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 0);
		final Amount nemesisFees = BlockExtensions.streamDefault(nemesisBlock)
				.map(Transaction::getFee)
				.reduce(Amount.ZERO, Amount::add);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(copy.getAccountCache().isKnownAddress(nemesisBlockInfo.getAddress()), IsEqual.equalTo(true));
		final AccountState nemesisState = copy.getAccountStateCache().findStateByAddress(nemesisBlockInfo.getAddress());
		Assert.assertThat(nemesisState.getAccountInfo().getBalance(), IsEqual.equalTo(nemesisFees));
		Assert.assertThat(nemesisState.getWeightedBalances().getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(nemesisState.getWeightedBalances().getUnvested(BlockHeight.ONE), IsEqual.equalTo(nemesisFees));
		Assert.assertThat(nemesisState.getHeight(), IsEqual.equalTo(BlockHeight.ONE));
	}

	@Test
	public void analyzeExecutesNemesisTransferChangesInNisCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 0);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS);

		// Assert:
		// - just test the balances of two prototype accounts
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(getBalance(copy, TEST_ADDRESS1), IsEqual.equalTo(Amount.fromNem(50000000L)));
		Assert.assertThat(getBalance(copy, TEST_ADDRESS2), IsEqual.equalTo(Amount.fromNem(50000000L)));
	}

	@Test
	public void analyzeExecutesPostNemesisTransferChangesInNisCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		final List<Block> blocks = NisUtils.createBlockList(nemesisBlock, 1);
		// TODO 20150818 J-B: failing because private key needs to be provided for signer, but i misplaced the testnet private keys file :/
		final Transaction transfer = new TransferTransaction(
				TimeInstant.ZERO,
				new Account(TEST_ADDRESS1),
				new Account(TEST_ADDRESS2),
				Amount.fromNem(1000000),
				null);
		transfer.setFee(Amount.fromNem(100));
		transfer.sign();
		blocks.get(0).addTransaction(transfer);
		blocks.get(0).sign();
		context.fillDatabase(nemesisBlock, blocks);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS);

		// Assert:
		// - just test the balances of two prototype accounts
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(getBalance(copy, TEST_ADDRESS1), IsEqual.equalTo(Amount.fromNem(48999900L)));
		Assert.assertThat(getBalance(copy, TEST_ADDRESS2), IsEqual.equalTo(Amount.fromNem(51000000L)));
	}

	@Test
	public void analyzeFailsIfNemesisBlockIsInconsistentWithDbBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		nemesisBlock.setPreviousGenerationHash(Utils.generateRandomHash());
		context.fillDatabase(nemesisBlock, 0);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, DEFAULT_OPTIONS);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(false));
	}

	private static Amount getBalance(final ReadOnlyNisCache cache, final Address address) {
		return cache.getAccountStateCache().findStateByAddress(address).getAccountInfo().getBalance();
	}

	private static void setNetwork(final NetworkInfo info) {
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(info);
	}

	private class TestContext {
		private final ImportanceCalculator importanceCalculator = Mockito.spy(new ImportanceCalculator() {
			@Override
			public void recalculate(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
				accountStates.stream().forEach(a -> a.getImportanceInfo().setImportance(blockHeight, 1.0 / accountStates.size()));
			}
		});
		private final ReadOnlyNisCache nisCache;
		private final MockAccountDao accountDao = Mockito.spy(new MockAccountDao());
		private final MockBlockDao blockDao = Mockito.spy(new MockBlockDao(MockBlockDao.MockBlockDaoMode.MultipleBlocks, this.accountDao));
		private final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapper(this.accountDao);
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.spy(new BlockChainLastBlockLayer(this.blockDao, this.mapper));
		private final BlockChainScoreManager scoreManager;
		private final NisMapperFactory nisMapperFactory;
		private final BlockAnalyzer blockAnalyzer;

		private TestContext() {
			final DefaultPoiFacade poiFacade = new DefaultPoiFacade(this.importanceCalculator);
			this.nisCache = NisCacheFactory.createReal(poiFacade);
			this.scoreManager = Mockito.spy(new MockBlockChainScoreManager(this.nisCache.getAccountStateCache()));
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			this.nisMapperFactory = Mockito.spy(new NisMapperFactory(mapperFactory));
			this.blockAnalyzer = new BlockAnalyzer(
					this.blockDao,
					this.scoreManager,
					this.blockChainLastBlockLayer,
					this.nisMapperFactory);
		}

		private void fillDatabase(final Block nemesisBlock, final int numBlocks) {
			final List<Block> blocks = NisUtils.createBlockList(nemesisBlock, numBlocks);
			this.fillDatabase(nemesisBlock, blocks);
		}

		private void fillDatabase(final Block nemesisBlock, final List<Block> blocks) {
			this.blockDao.save(MapperUtils.toDbModel(nemesisBlock, new AccountDaoLookupAdapter(this.accountDao)));
			blocks.forEach(b -> this.blockDao.save(MapperUtils.toDbModel(b, new AccountDaoLookupAdapter(this.accountDao))));
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
}
