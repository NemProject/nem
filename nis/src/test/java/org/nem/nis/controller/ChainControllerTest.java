package org.nem.nis.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.model.*;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.nis.BlockScorer;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.Transfer;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.poi.PoiAlphaImportanceGeneratorImpl;
import org.nem.nis.poi.PoiImportanceGenerator;
import org.nem.nis.poi.PoiScorer.ScoringAlg;
import org.nem.nis.service.*;
import org.nem.nis.test.*;

public class ChainControllerTest {

	@Test
	public void blockLastReturnsMappedBlockFromBlockChain() {
		// Arrange:
		final org.nem.nis.dbmodel.Block dbBlock = NisUtils.createDbBlockWithTimeStamp(443);
		final TestContext context = new TestContext();
		Mockito.when(context.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbBlock);

		// Act:
		final Block block = context.controller.blockLast();

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(443)));
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastDbBlock();
	}

	@Test
	public void hashesFromReturnsHashesFromHeight() {
		// Arrange:
		final int defaultLimit = BlockChainConstants.BLOCKS_LIMIT;
		final HashChain originalHashes = new HashChain(NisUtils.createHashesList(3));
		final TestContext context = new TestContext();
		Mockito.when(context.blockDao.getHashesFrom(new BlockHeight(44), defaultLimit)).thenReturn(originalHashes);

		// Act:
		final HashChain chain = context.controller.hashesFrom(new BlockHeight(44));

		// Assert:
		Assert.assertThat(chain, IsEqual.equalTo(originalHashes));
		Mockito.verify(context.blockDao, Mockito.times(1)).getHashesFrom(new BlockHeight(44), defaultLimit);
	}

	@Test
	public void chainScoreReturnsScore() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.blockChain.getScore()).thenReturn(new BlockChainScore(21));

		// Act:
		final BlockChainScore score = context.controller.chainScore();

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(new BlockChainScore(21)));
		Mockito.verify(context.blockChain, Mockito.times(1)).getScore();
	}


	@Test
	public void blockDebugInfoDelegatesToBlockDaoAndBlockScorer() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = new BlockHeight(10);
		//final Address address = Utils.generateRandomAddressWithPublicKey();
		final TimeInstant timestamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(new PoiAlphaImportanceGeneratorImpl());
		Account signer1 = accountAnalyzer.addAccountToCache(Utils.generateRandomAccount().getAddress());
		Account signer2 = accountAnalyzer.addAccountToCache(Utils.generateRandomAccount().getAddress());
		final Block blockDaoBlock = new Block(
				signer1,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timestamp.addSeconds(60),
				height);
		blockDaoBlock.setDifficulty(difficulty);
		blockDaoBlock.getSigner().setHeight(BlockHeight.ONE);
		final Block blockDaoParent = new Block(
				signer2,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timestamp,
				height.prev());
		blockDaoParent.setDifficulty(difficulty);
		blockDaoParent.getSigner().setHeight(BlockHeight.ONE);

		BlockScorer scorer = new BlockScorer(accountAnalyzer);
		scorer.forceImportanceCalculation();
		final BigInteger hit = scorer.calculateHit(blockDaoBlock);
		final BigInteger target = scorer.calculateTarget(blockDaoParent, blockDaoBlock);

		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block();
		dbBlock.setPrevBlockHash(blockDaoBlock.getPreviousBlockHash().getRaw());
		dbBlock.setGenerationHash(blockDaoBlock.getGenerationHash().getRaw());
		dbBlock.setForgerId(new org.nem.nis.dbmodel.Account("", blockDaoBlock.getSigner().getKeyPair().getPublicKey()));
		dbBlock.setDifficulty(blockDaoBlock.getDifficulty().getRaw());
		dbBlock.setHeight(blockDaoBlock.getHeight().getRaw());
		dbBlock.setTimestamp(blockDaoBlock.getTimeStamp().getRawTime());
		dbBlock.setForgerProof(new byte[64]);
		dbBlock.setBlockTransfers(new ArrayList<Transfer>());
		final org.nem.nis.dbmodel.Block dbParent = NisUtils.createDbBlockWithTimeStamp(383);
		dbParent.setPrevBlockHash(blockDaoParent.getPreviousBlockHash().getRaw());
		dbParent.setGenerationHash(blockDaoParent.getGenerationHash().getRaw());
		dbParent.setForgerId(new org.nem.nis.dbmodel.Account("", blockDaoBlock.getSigner().getKeyPair().getPublicKey()));
		dbParent.setDifficulty(blockDaoParent.getDifficulty().getRaw());
		dbParent.setHeight(blockDaoParent.getHeight().getRaw());
		dbParent.setTimestamp(blockDaoParent.getTimeStamp().getRawTime());
		dbParent.setForgerProof(new byte[64]);
		dbParent.setBlockTransfers(new ArrayList<Transfer>());
		Mockito.when(context.blockDao.findByHeight(new BlockHeight(10))).thenReturn(dbBlock);
		Mockito.when(context.blockDao.findByHeight(new BlockHeight(9))).thenReturn(dbParent);

		Mockito.when(context.blockChain.getAccountAnalyzerCopy()).thenReturn(accountAnalyzer);

		// Act:
		final BlockDebugInfo blockDebugInfo = context.controller.blockDebugInfo("10");

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForager(), IsEqual.equalTo(signer1.getAddress()));
		Assert.assertThat(blockDebugInfo.getTimestamp(), IsEqual.equalTo(timestamp.addSeconds(60)));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));
		Assert.assertThat(blockDebugInfo.getTarget(), IsEqual.equalTo(target));
		Assert.assertThat(blockDebugInfo.getInterBlockTime(), IsEqual.equalTo(60));

		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(10));
		Mockito.verify(context.blockDao, Mockito.times(1)).findByHeight(new BlockHeight(9));
		Mockito.verify(context.blockDao, Mockito.times(2)).findByHeight(Mockito.any());
	}
	
	private static class TestContext {
		private final RequiredBlockDao blockDao = Mockito.mock(RequiredBlockDao.class);
		private final MockAccountLookup accountLookup = new MockAccountLookup();
		private final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		private final BlockChain blockChain = Mockito.mock(BlockChain.class);
		private final ChainController controller;

		private TestContext() {
			this.controller = new ChainController(
					this.blockDao,
					this.accountLookup,
					this.blockChainLastBlockLayer,
					this.blockChain);
		}
	}
}
