package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.test.BlockUtils;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class BlockExplorerControllerTest {
	private static final int BLOCKS_LIMIT = 10;

	// region localBlocksAfter

	@Test
	public void localBlocksAfterDelegatesToBlockDao() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final TestContext context = new TestContext();

		// Act:
		final SerializableList<ExplorerBlockViewModel> blocks = context.controller.localBlocksAfter(height);

		// Assert:
		Mockito.verify(context.blockDao, Mockito.only()).getBlocksAfter(height, BLOCKS_LIMIT);
		MatcherAssert.assertThat(blocks.size(), IsEqual.equalTo(0));
	}

	@Test
	public void localBlocksAfterReturnsDelegatesToMapper() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final TestContext context = new TestContext();
		context.setBlocksWithHeight(height, 15, 16, 18);

		// Act:
		final SerializableList<ExplorerBlockViewModel> blocks = context.controller.localBlocksAfter(height);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(3)).map(Mockito.any(), Mockito.eq(ExplorerBlockViewModel.class));
		MatcherAssert.assertThat(blocks.size(), IsEqual.equalTo(3));
	}

	@Test
	public void localBlocksAfterReturnsAppropriateBlocks() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final TestContext context = new TestContext();
		context.setBlocksWithHeight(height, 15, 16, 18);

		// Act:
		final SerializableList<ExplorerBlockViewModel> blocks = context.controller.localBlocksAfter(height);

		// Assert:
		MatcherAssert.assertThat(blocks.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(blocks.asCollection().stream().map(BlockExplorerControllerTest::getHeight).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(15L, 16L, 18L)));
	}

	// endregion

	// region localBlockAt

	@Test
	public void localBlockAtReturnsRequestedBlock() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final DbBlock dbBlock = NisUtils.createDbBlockWithTimeStampAtHeight(0, 16);
		final TestContext context = new TestContext();
		Mockito.when(context.blockDao.findByHeight(height)).thenReturn(dbBlock);

		// Act:
		final ExplorerBlockViewModel block = context.controller.localBlockAt(height);

		// Assert:
		Mockito.verify(context.blockDao, Mockito.only()).findByHeight(height);
		Mockito.verify(context.mapper, Mockito.only()).map(dbBlock, ExplorerBlockViewModel.class);
		MatcherAssert.assertThat(block, IsNull.notNullValue());
		MatcherAssert.assertThat(getHeight(block), IsEqual.equalTo(16L));
	}

	// endregion

	private static class TestContext {
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final BlockExplorerController controller;

		public TestContext() {
			final MapperFactory mapperFactory = Mockito.mock(MapperFactory.class);
			final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
			Mockito.when(mapperFactory.createDbModelToModelMapper(accountLookup)).thenReturn(this.mapper);
			Mockito.when(this.mapper.map(Mockito.any(), Mockito.eq(ExplorerBlockViewModel.class))).then(invocationOnMock -> {
				final long height = ((DbBlock) invocationOnMock.getArguments()[0]).getHeight();
				final Block block = BlockUtils.createBlockWithHeight(new BlockHeight(height));
				block.sign();
				return new ExplorerBlockViewModel(block, Hash.ZERO);
			});
			this.controller = new BlockExplorerController(this.blockDao, mapperFactory, accountLookup);
		}

		public void setBlocksWithHeight(final BlockHeight height, final int... heights) {
			final List<DbBlock> dbBlocks = new ArrayList<>();
			for (int i = 0; i < heights.length; ++i) {
				dbBlocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(i, heights[i]));
			}

			Mockito.when(this.blockDao.getBlocksAfter(height, BLOCKS_LIMIT)).thenReturn(dbBlocks);
		}
	}

	private static long getHeight(final ExplorerBlockViewModel viewModel) {
		return (Long) ((JSONObject) JsonSerializer.serializeToJson(viewModel).get("block")).get("height");
	}
}
