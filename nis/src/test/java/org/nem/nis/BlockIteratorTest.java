package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.BlockHeight;
import org.nem.nis.test.MockBlockLookup;
import org.nem.nis.test.NisUtils;
import org.nem.nis.visitors.BlockVisitor;

import java.util.*;

public class BlockIteratorTest {

	@Test
	public void unwindDoesNotVisitAnyBlocksIfLastBlockHasDesiredHeight() {
		// Arrange:
		final MockBlockLookup lookup = new MockBlockLookup(NisUtils.createRandomBlockWithHeight(7));
		final MockBlockVisitor visitor = new MockBlockVisitor();

		// Act:
		BlockIterator.unwindUntil(lookup, new BlockHeight(7), visitor);

		// Assert:
		Assert.assertThat(visitor.visitedBlockHeights.size(), IsEqual.equalTo(0));
	}

	@Test
	public void unwindVisitsAllBlocksWithHeightGreaterThanDesiredHeight() {
		// Arrange:
		final MockBlockLookup lookup = new MockBlockLookup(NisUtils.createRandomBlockWithHeight(7));
		lookup.addBlock(NisUtils.createRandomBlockWithHeight(6));
		lookup.addBlock(NisUtils.createRandomBlockWithHeight(5));
		lookup.addBlock(NisUtils.createRandomBlockWithHeight(4));
		lookup.addBlock(NisUtils.createRandomBlockWithHeight(3));
		final MockBlockVisitor visitor = new MockBlockVisitor();

		// Act:
		BlockIterator.unwindUntil(lookup, new BlockHeight(4), visitor);

		// Assert:
		Assert.assertThat(visitor.visitedBlockHeights, IsEqual.equalTo(Arrays.asList(7L, 6L, 5L)));
		Assert.assertThat(visitor.visitedParentBlockHeights, IsEqual.equalTo(Arrays.asList(6L, 5L, 4L)));
	}

	@Test
	public void allVisitsAllBlocksInOrder() {
		// Arrange:
		final List<Block> blocks = new ArrayList<>();
		blocks.add(NisUtils.createRandomBlockWithHeight(7));
		blocks.add(NisUtils.createRandomBlockWithHeight(11));
		blocks.add(NisUtils.createRandomBlockWithHeight(8));

		final MockBlockVisitor visitor = new MockBlockVisitor();

		// Act:
		BlockIterator.all(NisUtils.createRandomBlockWithHeight(12), blocks, visitor);

		// Assert:
		Assert.assertThat(visitor.visitedBlockHeights, IsEqual.equalTo(Arrays.asList(7L, 11L, 8L)));
		Assert.assertThat(visitor.visitedParentBlockHeights, IsEqual.equalTo(Arrays.asList(12L, 7L, 11L)));
	}

	private class MockBlockVisitor implements BlockVisitor {

		private final List<Long> visitedParentBlockHeights = new ArrayList<>();
		private final List<Long> visitedBlockHeights = new ArrayList<>();

		@Override
		public void visit(final Block parentBlock, final Block block) {
			this.visitedParentBlockHeights.add(parentBlock.getHeight().getRaw());
			this.visitedBlockHeights.add(block.getHeight().getRaw());
		}
	}
}
