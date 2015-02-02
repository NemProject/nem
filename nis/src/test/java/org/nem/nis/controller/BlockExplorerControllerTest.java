package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.test.NisUtils;

import java.util.Arrays;

public class BlockExplorerControllerTest {

	/*
	@Test
	public void localBlocksAfterDelegatesToBlockDao() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		Mockito.when(blockDao.getBlocksAfter(height, BlockChainConstants.BLOCKS_LIMIT))
				.thenReturn(Arrays.asList());
		final BlockExplorerController controller = new BlockExplorerController(blockDao);

		// Act:
		final SerializableList<ExplorerBlockViewModel> blocks = controller.localBlocksAfter(height);

		// Assert:
		Mockito.verify(blockDao, Mockito.only()).getBlocksAfter(height, BlockChainConstants.BLOCKS_LIMIT);
		Assert.assertThat(blocks.size(), IsEqual.equalTo(0));
	}

	@Test
	public void localBlocksAfterReturnsAppropriateBlocks() {
		// Arrange:
		final BlockHeight height = new BlockHeight(14);
		final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		Mockito.when(blockDao.getBlocksAfter(height, BlockChainConstants.BLOCKS_LIMIT))
				.thenReturn(Arrays.asList(
						NisUtils.createDbBlockWithTimeStampAtHeight(1, 15),
						NisUtils.createDbBlockWithTimeStampAtHeight(2, 16),
						NisUtils.createDbBlockWithTimeStampAtHeight(3, 18)));
		final BlockExplorerController controller = new BlockExplorerController(blockDao);

		// Act:
		final SerializableList<ExplorerBlockViewModel> blocks = controller.localBlocksAfter(height);

		// Assert:
		Mockito.verify(blockDao, Mockito.only()).getBlocksAfter(height, BlockChainConstants.BLOCKS_LIMIT);
		Assert.assertThat(blocks.size(), IsEqual.equalTo(3));
		Assert.assertThat(getHeight(blocks.get(0)), IsEqual.equalTo(15L));
		Assert.assertThat(getHeight(blocks.get(1)), IsEqual.equalTo(16L));
		Assert.assertThat(getHeight(blocks.get(2)), IsEqual.equalTo(18L));
	}

	private static long getHeight(final ExplorerBlockViewModel viewModel) {
		return (Long)JsonSerializer.serializeToJson(viewModel).get("height");
	}
	*/
}