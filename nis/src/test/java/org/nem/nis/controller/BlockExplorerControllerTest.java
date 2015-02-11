package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
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

	@Test
	public void localBlocksAfterDelegatesToBlockDao() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final TestContext context = new TestContext();

		// Act:
		final SerializableList<ExplorerBlockViewModel> blocks = context.controller.localBlocksAfter(height);

		// Assert:
		Mockito.verify(context.blockDao, Mockito.only()).getBlocksAfter(height, BLOCKS_LIMIT);
		Assert.assertThat(blocks.size(), IsEqual.equalTo(0));
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
		Assert.assertThat(blocks.size(), IsEqual.equalTo(3));
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
		Assert.assertThat(blocks.size(), IsEqual.equalTo(3));
		Assert.assertThat(
				blocks.asCollection().stream().map(b -> getHeight(b)).collect(Collectors.toList()),
				IsEqual.equalTo(Arrays.asList(15L, 16L, 18L)));
	}

	private static class TestContext {
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final BlockExplorerController controller;

		public TestContext() {
			final MapperFactory mapperFactory = Mockito.mock(MapperFactory.class);
			final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
			Mockito.when(mapperFactory.createDbModelToModelMapper(accountLookup))
					.thenReturn(this.mapper);
			Mockito.when(this.mapper.map(Mockito.any(), Mockito.eq(ExplorerBlockViewModel.class)))
					.then(invocationOnMock -> {
						final long height = ((DbBlock)invocationOnMock.getArguments()[0]).getHeight();
						final Block block = BlockUtils.createBlockWithHeight(new BlockHeight(height));
						block.sign();
						return new ExplorerBlockViewModel(block, Hash.ZERO);
					});
			this.controller = new BlockExplorerController(this.blockDao, mapperFactory, accountLookup);
		}

		public List<DbBlock> setBlocksWithHeight(final BlockHeight height, final int... heights) {
			final List<DbBlock> dbBlocks = new ArrayList<>();
			for (int i = 0; i < heights.length; ++i) {
				dbBlocks.add(NisUtils.createDbBlockWithTimeStampAtHeight(i, heights[i]));
			}

			Mockito.when(this.blockDao.getBlocksAfter(height, BLOCKS_LIMIT))
					.thenReturn(dbBlocks);

			return dbBlocks;
		}
	}

	private static long getHeight(final ExplorerBlockViewModel viewModel) {
		return (Long)((JSONObject)JsonSerializer.serializeToJson(viewModel).get("block")).get("height");
	}
}