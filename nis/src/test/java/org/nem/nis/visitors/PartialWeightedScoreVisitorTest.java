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

		// Assert: score is initially zero
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(BlockChainScore.ZERO));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 1);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(1)));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 4);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(5)));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 14);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(19)));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 7);
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(26)));

		// Assert: scores are unchanged in-between visits
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(26)));
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
