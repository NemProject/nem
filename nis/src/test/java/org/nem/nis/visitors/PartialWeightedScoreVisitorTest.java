package org.nem.nis.visitors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.nis.test.MockBlockScorer;
import org.nem.nis.test.NisUtils;

public class PartialWeightedScoreVisitorTest {

	@Test
	public void scoresAreCalculatedCorrectlyForForwardChains() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final PartialWeightedScoreVisitor visitor = new PartialWeightedScoreVisitor(
				scorer,
				PartialWeightedScoreVisitor.BlockOrder.Forward);

		// Assert:
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(0L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 1);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(2L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 4);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(6L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 14);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(20L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 7);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(27L));
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(27L));
	}

	@Test
	public void scoresAreCalculatedCorrectlyForReverseChains() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final PartialWeightedScoreVisitor visitor = new PartialWeightedScoreVisitor(
				scorer,
				PartialWeightedScoreVisitor.BlockOrder.Reverse);

		// Assert:
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(0L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 1);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(2L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 4);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(9L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 14);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(33L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 6);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(31L));
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(31L));
	}

	private static void visitBlockWithScore(final BlockVisitor visitor, final MockBlockScorer scorer, final long score) {
		// Arrange:
		final Block block = NisUtils.createRandomBlock();
		final Block parentBlock = NisUtils.createRandomBlock();
		scorer.setBlockScore(parentBlock, block, score);

		// Act:
		visitor.visit(parentBlock, block);
	}
}
