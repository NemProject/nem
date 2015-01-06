package org.nem.nis.service;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisModelToDbModelMapper;
import org.nem.nis.test.*;

public class BlockChainLastBlockLayerTest {

	@Test
	public void uninitializedLastBlockLayerReturnsNull() {
		// Act:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();

		// Assert:
		Assert.assertNull(lastBlockLayer.getLastDbBlock());
	}

	@Test
	public void afterAnalyzeLastBlockLayerReturnsBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();
		final DbBlock block = this.createDbBlock(1);

		// Act:
		lastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
	}

	// TODO 20141230 J-B,G: should rename this test!
	@Test
	public void foo() {
		// Arrange:
		final MockAccountDao accountDao = new MockAccountDao();
		final MockBlockDao mockBlockDao = new MockBlockDao(null);
		final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapper(accountDao);
		final BlockChainLastBlockLayer lastBlockLayer = new BlockChainLastBlockLayer(mockBlockDao, mapper);
		final org.nem.core.model.Block lastBlock = createBlock();
		final DbBlock lastDbBlock = mapper.map(lastBlock);

		// Act:
		lastBlockLayer.analyzeLastBlock(lastDbBlock);
		final DbBlock result1 = lastBlockLayer.getLastDbBlock();

		final org.nem.core.model.Block nextBlock = createBlock();
		lastBlockLayer.addBlockToDb(nextBlock);

		// Assert:
		Assert.assertThat(result1, IsSame.sameInstance(lastDbBlock));
		final DbBlock last = mockBlockDao.getLastSavedBlock();
		Assert.assertThat(last.getId(), IsEqual.equalTo(1L));
		Assert.assertThat(new Signature(last.getHarvesterProof()), IsEqual.equalTo(nextBlock.getSignature()));
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsEqual.equalTo(last));
	}

	private static org.nem.core.model.Block createBlock(final Account forger) {
		// Arrange:
		final org.nem.core.model.Block block = new org.nem.core.model.Block(forger,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				new TimeInstant(7),
				new BlockHeight(3));
		block.sign();
		return block;
	}

	private static org.nem.core.model.Block createBlock() {
		// Arrange:
		return createBlock(Utils.generateRandomAccount());
	}

	private DbBlock createDbBlock(final long i) {
		final DbBlock block = new DbBlock();
		block.setShortId(i);
		return block;
	}

	private BlockChainLastBlockLayer createBlockChainLastBlockLayer() {
		final MockAccountDao accountDao = new MockAccountDao();
		final MockBlockDao mockBlockDao = new MockBlockDao(null);
		final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapper(accountDao);
		return new BlockChainLastBlockLayer(mockBlockDao, mapper);
	}
}
