package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Block;
import org.nem.core.model.BlockHeight;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.MockBlockLookup;
import org.nem.nis.test.MockBlockScorer;
import org.nem.nis.test.NisUtils;
import org.nem.nis.visitors.BlockVisitor;

import java.util.*;

public class BlockIteratorTest {

	@Test
	public void unwindDoesNotVisitAnyBlocksIfLastBlockHasDesiredHeight() {
		// Arrange:
		final MockBlockLookup lookup = new MockBlockLookup(NisUtils.createRandomBlock(7));
		final MockBlockVisitor visitor = new MockBlockVisitor();

		// Act:
		BlockIterator.unwindUntil(lookup, new BlockHeight(7), visitor);

		// Assert:
		Assert.assertThat(visitor.visitedBlockHeights.size(), IsEqual.equalTo(0));
	}

	@Test
	public void unwindVisitsAllBlocksWithHeightGreaterThanDesiredHeight() {
		// Arrange:
		final MockBlockLookup lookup = new MockBlockLookup(NisUtils.createRandomBlock(7));
		lookup.addBlock(NisUtils.createRandomBlock(6));
		lookup.addBlock(NisUtils.createRandomBlock(5));
		lookup.addBlock(NisUtils.createRandomBlock(4));
		lookup.addBlock(NisUtils.createRandomBlock(3));
		final MockBlockVisitor visitor = new MockBlockVisitor();

		// Act:
		BlockIterator.unwindUntil(lookup, new BlockHeight(4), visitor);

		// Assert:
		Assert.assertThat(visitor.visitedBlockHeights, IsEqual.equalTo(Arrays.asList(7L, 6L, 5L)));
	}

	@Test
	public void allVisitsAllBlocksInOrder() {
		// Arrange:
		final List<Block> blocks = new ArrayList<>();
		blocks.add(NisUtils.createRandomBlock(7));
		blocks.add(NisUtils.createRandomBlock(11));
		blocks.add(NisUtils.createRandomBlock(8));

		final MockBlockVisitor visitor = new MockBlockVisitor();

		// Act:
		BlockIterator.all(blocks, visitor);

		// Assert:
		Assert.assertThat(visitor.visitedBlockHeights, IsEqual.equalTo(Arrays.asList(7L, 11L, 8L)));
	}

	private class MockBlockVisitor implements BlockVisitor {

		private final List<Long> visitedBlockHeights = new ArrayList<>();

		@Override
		public void visit(final Block block) {
			this.visitedBlockHeights.add(block.getHeight().getRaw());
		}
	}
}
