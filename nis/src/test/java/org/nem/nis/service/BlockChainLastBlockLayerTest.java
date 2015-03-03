package org.nem.nis.service;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisModelToDbModelMapper;
import org.nem.nis.test.*;

public class BlockChainLastBlockLayerTest {

	//region basic operations

	@Test
	public void layerIsInitializedWithNoLastBlock() {
		// Act:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsNull.nullValue());
		Assert.assertThat(lastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(lastBlockLayer.isLoading(), IsEqual.equalTo(true));
	}

	@Test
	public void analyzeLastBlockSetsBlockAsLastBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();
		final DbBlock block = createDbBlock(123);

		// Act:
		lastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
		Assert.assertThat(lastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(lastBlockLayer.isLoading(), IsEqual.equalTo(true));
	}

	@Test
	public void setLoadedChangesIsLoadingStatusToFalse() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();

		// Act:
		lastBlockLayer.setLoaded();

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsNull.nullValue());
		Assert.assertThat(lastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(lastBlockLayer.isLoading(), IsEqual.equalTo(false));
	}

	@Test
	public void analyzeLastBlockSetsBlockAsLastBlockWhenIsLoadingIsFalse() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();
		final DbBlock block = createDbBlock(123);

		// this is here, so that one of the LOGS, won't fail, it's not part of test itself
		block.setBlockHash(Utils.generateRandomHash());

		// Act:
		lastBlockLayer.setLoaded();
		lastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
		Assert.assertThat(lastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(lastBlockLayer.isLoading(), IsEqual.equalTo(false));
	}

	//endregion

	//region addBlockToDb

	@Test
	public void addBlockToDbDelegatesToBlockDaoAndMapper() {
		// Arrange:
		final Block block = createBlock(777);
		final DbBlock dbBlock = createDbBlock(777);
		final TestContext context = new TestContext();
		Mockito.when(context.mapper.map(block)).thenReturn(dbBlock);

		// Act:
		context.lastBlockLayer.addBlockToDb(block);

		// Assert:
		Mockito.verify(context.mapper, Mockito.only()).map(block);
		Mockito.verify(context.mockBlockDao, Mockito.only()).save(dbBlock);
	}

	@Test
	public void addBlockToDbUpdatesLastBlock() {
		// Arrange:
		final Block block = createBlock(777);
		final DbBlock dbBlock = createDbBlock(777);
		final TestContext context = new TestContext();
		Mockito.when(context.mapper.map(block)).thenReturn(dbBlock);

		// Act:
		context.lastBlockLayer.addBlockToDb(block);

		// Assert:
		Assert.assertThat(context.lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(dbBlock));
		Assert.assertThat(context.lastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(777)));
		Assert.assertThat(context.lastBlockLayer.isLoading(), IsEqual.equalTo(false));
	}

	//endregion

	//region dropDbBlocksAfter

	@Test
	public void dropDbBlocksAfterDelegatesToBlockDao() {
		// Arrange:
		final BlockHeight height = new BlockHeight(777);
		final TestContext context = new TestContext();

		// Act:
		context.lastBlockLayer.dropDbBlocksAfter(height);

		// Assert:
		Mockito.verify(context.mockBlockDao, Mockito.times(1)).deleteBlocksAfterHeight(height);
		Mockito.verify(context.mockBlockDao, Mockito.times(1)).findByHeight(height);
	}

	@Test
	public void dropDbBlocksAfterUpdatesLastBlock() {
		// Arrange:
		final BlockHeight height = new BlockHeight(777);
		final DbBlock block = createDbBlock(777);
		final TestContext context = new TestContext();
		Mockito.when(context.mockBlockDao.findByHeight(height)).thenReturn(block);

		// Act:
		context.lastBlockLayer.dropDbBlocksAfter(height);

		// Assert:
		Assert.assertThat(context.lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
		Assert.assertThat(context.lastBlockLayer.getLastBlockHeight(), IsEqual.equalTo(new BlockHeight(777)));
		Assert.assertThat(context.lastBlockLayer.isLoading(), IsEqual.equalTo(false));
	}

	//endregion

	//region helper functions

	private static org.nem.core.model.Block createBlock(final long height) {
		// Arrange:
		return NisUtils.createRandomBlockWithHeight(height);
	}

	private static DbBlock createDbBlock(final long height) {
		final DbBlock block = new DbBlock();
		block.setHeight(height);
		return block;
	}

	private BlockChainLastBlockLayer createBlockChainLastBlockLayer() {
		final MockAccountDao accountDao = new MockAccountDao();
		final MockBlockDao mockBlockDao = new MockBlockDao(null);
		final NisModelToDbModelMapper mapper = MapperUtils.createModelToDbModelNisMapper(accountDao);
		return new BlockChainLastBlockLayer(mockBlockDao, mapper);
	}

	private static class TestContext {
		private final BlockDao mockBlockDao = Mockito.mock(BlockDao.class);
		private final NisModelToDbModelMapper mapper = Mockito.mock(NisModelToDbModelMapper.class);
		private final BlockChainLastBlockLayer lastBlockLayer = new BlockChainLastBlockLayer(
				this.mockBlockDao,
				this.mapper);

		public TestContext() {
			this.lastBlockLayer.setLoaded();
		}
	}

	//endregion
}
