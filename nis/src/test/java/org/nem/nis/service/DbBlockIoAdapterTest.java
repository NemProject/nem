package org.nem.nis.service;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.test.NisUtils;

import java.util.MissingResourceException;

public class DbBlockIoAdapterTest {
	private static final long VALID_BLOCK_HEIGHT = 5;

	@Test
	public void getBlockDelegatesToBlockDao() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final TestContext context = new TestContext();
		Mockito.when(context.blockDao.findByHash(hash)).thenReturn(context.block);

		// Act:
		final Block block = context.blockIo.getBlock(hash);

		// Assert:
		Assert.assertThat(block.getHeight().getRaw(), IsEqual.equalTo(VALID_BLOCK_HEIGHT));
		Mockito.verify(context.blockDao, Mockito.only()).findByHash(hash);
	}

	@Test(expected = MissingResourceException.class)
	public void getBlockThrowsExceptionIfBlockCannotBeFound() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final TestContext context = new TestContext();

		// Act:
		context.blockIo.getBlock(hash);
	}

	@Test
	public void getBlockAtDelegatesToBlockDao() {
		// Arrange:
		final BlockHeight height = new BlockHeight(VALID_BLOCK_HEIGHT);
		final TestContext context = new TestContext();
		Mockito.when(context.blockDao.findByHeight(height)).thenReturn(context.block);

		// Act:
		final Block block = context.blockIo.getBlockAt(height);

		// Assert:
		Assert.assertThat(block.getHeight().getRaw(), IsEqual.equalTo(VALID_BLOCK_HEIGHT));
		Mockito.verify(context.blockDao, Mockito.only()).findByHeight(height);
	}

	@Test(expected = MissingResourceException.class)
	public void getBlockAtThrowsExceptionIfBlockCannotBeFound() {
		// Arrange:
		final BlockHeight height = new BlockHeight(8);
		final TestContext context = new TestContext();

		// Act:
		context.blockIo.getBlockAt(height);
	}

	private static class TestContext {
		private final org.nem.nis.dbmodel.Block block = NisUtils.createDbBlockWithTimeStampAtHeight(1, VALID_BLOCK_HEIGHT);
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final BlockIo blockIo = new DbBlockIoAdapter(this.blockDao, new MockAccountLookup());
	}
}