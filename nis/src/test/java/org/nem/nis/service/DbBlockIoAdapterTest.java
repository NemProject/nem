package org.nem.nis.service;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.MockAccountLookup;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.test.*;

import java.util.MissingResourceException;

public class DbBlockIoAdapterTest {
	private static final long VALID_BLOCK_HEIGHT = 5;

	@Test
	public void getBlockAtDelegatesToBlockDao() {
		// Arrange:
		final BlockHeight height = new BlockHeight(VALID_BLOCK_HEIGHT);
		final TestContext context = new TestContext();
		Mockito.when(context.blockDao.findByHeight(height)).thenReturn(context.block);

		// Act:
		final Block block = context.blockIo.getBlockAt(height);

		// Assert:
		MatcherAssert.assertThat(block.getHeight().getRaw(), IsEqual.equalTo(VALID_BLOCK_HEIGHT));
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
		private final DbBlock block = NisUtils.createDbBlockWithTimeStampAtHeight(1, VALID_BLOCK_HEIGHT);
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final BlockIo blockIo = new DbBlockIoAdapter(this.blockDao,
				MapperUtils.createDbModelToModelNisMapper(new MockAccountLookup()));
	}
}
