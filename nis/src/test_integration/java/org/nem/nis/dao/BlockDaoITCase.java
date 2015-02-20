package org.nem.nis.dao;

import org.junit.Test;
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
	private static final Logger LOGGER = Logger.getLogger(TransferDaoITCase.class.getName());

	@Autowired
	BlockDao blockDao;

	@Autowired
	TestDatabase database;

	@Test
	public void getBlocksAfterItCase() {
		System.gc();
		final long originalTime = measureTime(this.blockDao::getBlocksAfterOriginal);

		System.gc();
		final long newTime = measureTime(this.blockDao::getBlocksAfter);

		// TODO 20150217 J-B: on my machine, i'm seeing times like:
		// WARNING: getBlocksAfter needed 6410 ms(orig) and 9625 ms(new)

		// TODO 20150217 J-B: the difference is also pronounced when processing just transfers:
		// WARNING: getBlocksAfter needed 4557ms (orig) and 6802ms
		// i will leave you with that
		LOGGER.warning(String.format("getBlocksAfter needed %dms (orig) and %dms (new)", originalTime, newTime));
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
