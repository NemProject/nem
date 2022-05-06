package org.nem.nis.visitors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockChainScore;
import org.nem.nis.test.*;

public class PartialWeightedScoreVisitorTest {

	@Test
	public void scoresAreCalculatedCorrectly() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final PartialWeightedScoreVisitor visitor = new PartialWeightedScoreVisitor(scorer);

		// Assert: score is initially zero
		MatcherAssert.assertThat(visitor.getScore(), IsEqual.equalTo(BlockChainScore.ZERO));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 1);
		MatcherAssert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(1)));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 4);
		MatcherAssert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(5)));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 14);
		MatcherAssert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(19)));

		// Act / Assert:
		visitBlockWithScore(visitor, scorer, 7);
		MatcherAssert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(26)));

		// Assert: scores are unchanged in-between visits
		MatcherAssert.assertThat(visitor.getScore(), IsEqual.equalTo(new BlockChainScore(26)));
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
