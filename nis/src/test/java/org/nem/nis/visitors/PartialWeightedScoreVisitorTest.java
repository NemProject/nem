package org.nem.nis.visitors;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.nis.test.MockBlockScorer;
import org.nem.nis.test.NisUtils;

public class PartialWeightedScoreVisitorTest {

	@Test
	public void scoresAreCalculatedCorrectly() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final PartialWeightedScoreVisitor visitor = new PartialWeightedScoreVisitor(scorer);

		// Assert:
		Assert.assertThat(visitor.getScore().getRaw(), IsEqual.equalTo(0L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 1);
		Assert.assertThat(visitor.getScore().getRaw(), IsEqual.equalTo(1L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 4);
		Assert.assertThat(visitor.getScore().getRaw(), IsEqual.equalTo(5L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 14);
		Assert.assertThat(visitor.getScore().getRaw(), IsEqual.equalTo(19L));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 7);
		Assert.assertThat(visitor.getScore().getRaw(), IsEqual.equalTo(26L));
		Assert.assertThat(visitor.getScore().getRaw(), IsEqual.equalTo(26L));
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
