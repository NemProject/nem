package org.nem.nis.visitors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Block;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.NisUtils;

import java.util.*;

public class AggregateBlockVisitorTest {

	@Test
	public void aggregateDelegatesToAllAggregatedVisitors() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block parentBlock = NisUtils.createRandomBlock();
		final Block block = NisUtils.createRandomBlock();
		context.visitor.visit(parentBlock, block);

		// Assert:
		MatcherAssert.assertThat(context.visitor1.lastBlock, IsEqual.equalTo(block));
		MatcherAssert.assertThat(context.visitor1.lastParentBlock, IsEqual.equalTo(parentBlock));
		MatcherAssert.assertThat(context.visitor2.lastBlock, IsEqual.equalTo(block));
		MatcherAssert.assertThat(context.visitor2.lastParentBlock, IsEqual.equalTo(parentBlock));
	}

	@Test
	public void aggregateDelegatesToAllAggregatedVisitorsInOrder() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Block block = NisUtils.createRandomBlock();
		context.visitor.visit(null, block);

		// Assert:
		MatcherAssert.assertThat(context.visitList, IsEquivalent.equivalentTo(1, 2));
	}

	private static class TestContext {

		private final MockVisitor visitor1;
		private final MockVisitor visitor2;
		private final AggregateBlockVisitor visitor;
		private final List<Integer> visitList;

		public TestContext() {
			this.visitList = new ArrayList<>();
			this.visitor1 = new MockVisitor(1, this.visitList);
			this.visitor2 = new MockVisitor(2, this.visitList);

			final List<BlockVisitor> mockVisitors = new ArrayList<>();
			mockVisitors.add(this.visitor1);
			mockVisitors.add(this.visitor2);

			this.visitor = new AggregateBlockVisitor(mockVisitors);
		}
	}

	private static class MockVisitor implements BlockVisitor {

		private final int id;
		private final List<Integer> visitList;
		private Block lastBlock;
		private Block lastParentBlock;

		public MockVisitor(final int id, final List<Integer> visitList) {
			this.id = id;
			this.visitList = visitList;
		}

		@Override
		public void visit(final Block parentBlock, final Block block) {
			this.visitList.add(this.id);
			this.lastParentBlock = parentBlock;
			this.lastBlock = block;
		}
	}
}
