package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.HashChain;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChain;
import org.nem.nis.controller.utils.RequiredBlockDaoAdapter;
import org.nem.nis.test.*;

import java.util.List;

public class ChainControllerTest {

	@Test
	public void blockLastReturnsMappedBlockFromBlockChain() {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(null);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final MockBlockChain blockChain = new MockBlockChain(NisUtils.createBlockWithTimeStamp(443));
		final ChainController controller = new ChainController(requiredBlockDao, accountLookup, blockChain);

		// Act:
		final org.nem.core.model.Block block = controller.blockLast();

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(443)));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockChain.getNumGetLastDbBlockCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void hashesFromReturnsHashesFromHeight() {
		// Arrange:
		final List<byte[]> originalHashes = NisUtils.createRawHashesList(3);
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockBlockDao blockDao = new MockBlockDao(null, originalHashes);
		final RequiredBlockDaoAdapter requiredBlockDao = new RequiredBlockDaoAdapter(blockDao);
		final MockBlockChain blockChain = new MockBlockChain();
		final ChainController controller = new ChainController(requiredBlockDao, accountLookup, blockChain);

		// Act:
		final HashChain chain = controller.hashesFrom(NisUtils.getHeightDeserializer(44));

		// Assert:
		Assert.assertThat(chain.findFirstDifferent(new HashChain(originalHashes)), IsEqual.equalTo(3));
		Assert.assertThat(blockDao.getNumGetHashesFromCalls(), IsEqual.equalTo(1));
		Assert.assertThat(blockDao.getLastGetHashesFromHeight(), IsEqual.equalTo(44L));
		Assert.assertThat(blockDao.getLastGetHashesFromLimit(), IsEqual.equalTo(BlockChain.BLOCKS_LIMIT));
	}
}