package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.BlockChainScore;
import org.nem.core.model.BlockHeight;
import org.nem.core.crypto.HashChain;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChain;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.service.RequiredBlockDaoAdapter;
import org.nem.nis.test.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChainControllerTest {

	@Test
	public void blockLastReturnsMappedBlockFromBlockChain() {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(null);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final BlockChainLastBlockLayer blockChainLastBlockLayer = mock(BlockChainLastBlockLayer.class);
		final BlockChain blockChain = mock(BlockChain.class);
		when(blockChainLastBlockLayer.getLastDbBlock()).thenReturn(NisUtils.createBlockWithTimeStamp(443));
		final ChainController controller = new ChainController(requiredBlockDao, accountLookup, blockChainLastBlockLayer, blockChain);

		// Act:
		final org.nem.core.model.Block block = controller.blockLast();

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(443)));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
		Mockito.verify(blockChainLastBlockLayer, Mockito.times(1)).getLastDbBlock();
	}

	@Test
	public void hashesFromReturnsHashesFromHeight() {
		// Arrange:
		final HashChain originalHashes = new HashChain(NisUtils.createHashesList(3));
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(null, originalHashes);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final BlockChain blockChain = mock(BlockChain.class);
		final ChainController controller = new ChainController(requiredBlockDao, accountLookup, null, blockChain);

		// Act:
		final HashChain chain = controller.hashesFrom(new BlockHeight(44));

		// Assert:
		Assert.assertThat(chain, IsEqual.equalTo(originalHashes));
		Assert.assertThat(blockDao.getNumGetHashesFromCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastGetHashesFromHeight(), IsEqual.equalTo(new BlockHeight(44)));
		Assert.assertThat(blockDao.getLastGetHashesFromLimit(), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
	}

	@Test
	public void chainScoreReturnsScore() {
		// Arrange:
		final BlockChain blockChain = mock(BlockChain.class);
		when(blockChain.getScore()).thenReturn(new BlockChainScore(21));
		final ChainController controller = new ChainController(null, null, null, blockChain);

		// Act:
		final BlockChainScore score = controller.chainScore();

		// Assert:
		Assert.assertThat(score, IsEqual.equalTo(new BlockChainScore(21)));
		Mockito.verify(blockChain, Mockito.times(1)).getScore();
	}
}
