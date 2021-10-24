package org.nem.nis.dao;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.runner.RunWith;
import org.nem.core.model.primitive.BlockHeight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

@ContextConfiguration(classes = TestConfHardDisk.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class BlockDaoITCase {
	private static final Logger LOGGER = Logger.getLogger(BlockDaoITCase.class.getName());

	@Autowired
	BlockDao blockDao;

	@Autowired
	TestDatabase database;

	@Test
	public void getBlocksAfterItCase() {
		// Act:
		System.gc();
		final long elapsedTime = measureTime(this.blockDao::getBlocksAfter);
		LOGGER.warning(String.format("getBlocksAfter needed %dms", elapsedTime));

		// Assert:
		MatcherAssert.assertThat(elapsedTime < 7500, IsEqual.equalTo(true));
	}

	private static long measureTime(final BiConsumer<BlockHeight, Integer> getBlocks) {
		final int batchSize = 100;
		final long start = System.currentTimeMillis();
		for (int i = 1; i < TestDatabase.NUM_BLOCKS; i += batchSize) {
			getBlocks.accept(new BlockHeight(i), batchSize);
		}

		final long stop = System.currentTimeMillis();
		return stop - start;
	}
}
