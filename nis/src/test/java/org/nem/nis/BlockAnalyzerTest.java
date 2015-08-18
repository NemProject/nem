package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.ImportanceCalculator;
import org.nem.nis.secret.ObserverOption;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.BlockChainScoreManager;
import org.nem.nis.test.*;

import java.util.*;

public class BlockAnalyzerTest {

	@Test
	public void loadNemesisBlockReturnsExpectedBlockForTestNetwork() {
		// Arrange:
		final TestContext context = new TestContext();
		final NemesisBlockInfo nemesisBlockInfo = NetworkInfos.getDefault().getNemesisBlockInfo();

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
		final NemesisBlockInfo nemesisBlockInfo = NetworkInfos.getDefault().getNemesisBlockInfo();

		// Act:
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();

		// Assert:
		Assert.assertThat(nemesisBlock.getGenerationHash(), IsEqual.equalTo(nemesisBlockInfo.getGenerationHash()));
		Assert.assertThat(nemesisBlock.getSigner().getAddress(), IsEqual.equalTo(nemesisBlockInfo.getAddress()));
		Assert.assertThat(nemesisBlock.getTransactions().size(), IsEqual.equalTo(1353));
		setNetwork(NetworkInfos.getTestNetworkInfo());
	}

	@Test
	public void analyzeWithNullMaxHeightLoadsCompleteDb() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 345);
		final EnumSet<ObserverOption> options = EnumSet.of(ObserverOption.NoIncrementalPoi);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, options);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(context.blockChainLastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(346)));
	}

	// TODO 20150818 BR -> *: we should probably always call blockChainLastBlockLayer.analyzeLastBlock() after finishing loading to make this test pass.
	// > (Though I think we are not using the limited load anyway)
	@Test
	public void analyzeWithMaxHeightLoadsBlocksUpToMaxHeight() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache copy = context.nisCache.copy();
		final Block nemesisBlock = context.blockAnalyzer.loadNemesisBlock();
		context.fillDatabase(nemesisBlock, 345);
		final EnumSet<ObserverOption> options = EnumSet.of(ObserverOption.NoIncrementalPoi);

		// Act:
		final boolean success = context.blockAnalyzer.analyze(copy, options, 234L);

		// Assert:
		Assert.assertThat(success, IsEqual.equalTo(true));
		Assert.assertThat(context.blockChainLastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(234)));
	}

	private static void setNetwork(final NetworkInfo info) {
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(info);
	}

	private class TestContext {
		private final ReadOnlyNisCache nisCache;
		private final MockAccountDao accountDao = Mockito.spy(new MockAccountDao());
		private final MockBlockDao blockDao = Mockito.spy(new MockBlockDao(MockBlockDao.MockBlockDaoMode.MultipleBlocks, this.accountDao));
		private final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapper(this.accountDao);
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.spy(new BlockChainLastBlockLayer(this.blockDao, this.mapper));
		private final BlockChainScoreManager scoreManager;
		private final NisMapperFactory nisMapperFactory;
		private final BlockAnalyzer blockAnalyzer;

		private TestContext() {
			final ImportanceCalculator importanceCalculator = (blockHeight, accountStates) ->
					accountStates.stream().forEach(a -> a.getImportanceInfo().setImportance(blockHeight, 1.0 / accountStates.size()));
			final DefaultPoiFacade poiFacade = new DefaultPoiFacade(importanceCalculator);
			this.nisCache = NisCacheFactory.createReal(poiFacade);
			this.scoreManager = new MockBlockChainScoreManager(this.nisCache.getAccountStateCache());
			final MapperFactory mapperFactory = MapperUtils.createMapperFactory();
			this.nisMapperFactory = new NisMapperFactory(mapperFactory);
			this.blockAnalyzer = new BlockAnalyzer(
					this.blockDao,
					this.scoreManager,
					this.blockChainLastBlockLayer,
					this.nisMapperFactory);
		}

		private void fillDatabase(final Block nemesisBlock, final int numBlocks) {
			this.blockDao.save(MapperUtils.toDbModel(nemesisBlock, new AccountDaoLookupAdapter(this.accountDao)));
			final List<Block> blocks = NisUtils.createBlockList(nemesisBlock, numBlocks);
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
