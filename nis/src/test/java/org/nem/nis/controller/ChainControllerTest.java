package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChain;
import org.nem.nis.service.*;
import org.nem.nis.service.RequiredBlockDao;
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

	private static class TestContext {
		private final RequiredBlockDao blockDao = Mockito.mock(RequiredBlockDao.class);
		private final AccountLookup accountLookup = new MockAccountLookup();
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
