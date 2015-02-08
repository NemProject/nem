package org.nem.nis.service;

import org.hamcrest.core.*;
import org.junit.*;
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
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsNull.nullValue());
		Assert.assertThat(lastBlockLayer.getCurrentDbBlock(), IsNull.nullValue());
	}

	@Test
	public void afterAnalyzeLastBlockLayerReturnsBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();
		final DbBlock block = createDbBlock(1);

		// Act:
		lastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
		Assert.assertThat(lastBlockLayer.getCurrentDbBlock(), IsNull.nullValue());
	}

	@Test
	public void addBlockToDbSetsBlockIdOfLastBlock() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final DbBlock blockDaoLastBlock = context.addSingleBlockToDb();

		// Assert:
		Assert.assertThat(blockDaoLastBlock.getId(), IsEqual.equalTo(1L));
	}

	@Test
	public void addBlockToDbSavesBlockInBlockDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final DbBlock blockDaoLastBlock = context.addSingleBlockToDb();

		// Assert:
		Assert.assertThat(context.lastBlockLayer.getLastDbBlock(), IsEqual.equalTo(blockDaoLastBlock));
		Assert.assertThat(context.lastBlockLayer.getCurrentDbBlock(), IsNull.nullValue());
	}

	@Test
	public void setCurrentBlockOnlySetsCurrentBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();
		final DbBlock block = createDbBlock(1);

		// Act:
		lastBlockLayer.setCurrentBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsNull.nullValue());
		Assert.assertThat(lastBlockLayer.getCurrentDbBlock(), IsSame.sameInstance(block));

	}

	private static org.nem.core.model.Block createBlock(final Account harvester) {
		// Arrange:
		final org.nem.core.model.Block block = new org.nem.core.model.Block(
				harvester,
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

	private static DbBlock createDbBlock(final long i) {
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

	private static class TestContext {
		private final MockAccountDao accountDao = new MockAccountDao();
		private final MockBlockDao mockBlockDao = new MockBlockDao(createDbBlock(0));
		private final BlockChainLastBlockLayer lastBlockLayer = new BlockChainLastBlockLayer(
				this.mockBlockDao,
				MapperUtils.createModelToDbModelNisMapper(this.accountDao));

		private DbBlock addSingleBlockToDb() {
			// Arrange:
			final DbBlock lastBlockLayerLastBlock = createDbBlock(1);
			this.lastBlockLayer.analyzeLastBlock(lastBlockLayerLastBlock);

			// Act:
			final org.nem.core.model.Block nextBlock = createBlock();
			this.lastBlockLayer.addBlockToDb(nextBlock);
			return this.mockBlockDao.getLastSavedBlock();
		}
	}
}
