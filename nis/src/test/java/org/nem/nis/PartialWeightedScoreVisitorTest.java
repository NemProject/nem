package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.MockAccountLookup;
import org.nem.nis.dbmodel.Block;

public class PartialWeightedScoreVisitorTest {

	@Test
	public void scoreIsInitiallyZero() {
		// Arrange:
		final PartialWeightedScoreVisitor visitor = createVisitor();

		// Assert:
		Assert.assertThat(visitor.getScore(), IsEqual.equalTo(0L));
	}

	@Test
	public void scoresAreCalculatedCorrectlyForChains() {
		// Assert:
		Assert.assertThat(calculatePartialScore(new long[] { 1, 4, 14, 2 }), IsEqual.equalTo(23L));
		Assert.assertThat(calculatePartialScore(new long[] { 90, 11, 50 }), IsEqual.equalTo(201L));
	}

	private static PartialWeightedScoreVisitor createVisitor() {
		// Arrange:
		return new PartialWeightedScoreVisitor(null, null);
	}

	private static long calculatePartialScore(final long[] scores) {
		// Arrange:
		final PartialWeightedScoreVisitor visitor = new PartialWeightedScoreVisitor(
				new MockBlockScorer(scores),
				new MockAccountLookup());
		for (long ignored : scores)
			visitor.visit(new Block(), new Block());

		return visitor.getScore();
	}

	private static class MockBlockScorer extends BlockScorer {

		final long[] scores;
		int scoreIndex;

		public MockBlockScorer(final long[] scores) {
			this.scores = scores;
		}

		@Override
		public long calculateBlockScore(
				final AccountLookup accountLookup,
				final Block dbParentBlock,
				final Block dbBlock) {
			return this.scores[this.scoreIndex++];
		}
	}
}
