package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;

public class TrustScoresTest {

	@Test
	public void previouslyUnknownWeightCanBeRetrieved() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("bob");
		final TrustScores scores = new TrustScores();

		// Act:
		final RealDouble weight = scores.getScoreWeight(node);

		// Assert:
		MatcherAssert.assertThat(weight.get(), IsEqual.equalTo(0.0));
	}

	@Test
	public void sameWeightIsReturnedForSameNode() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("bob");
		final TrustScores scores = new TrustScores();

		// Act:
		final RealDouble weight1 = scores.getScoreWeight(node);
		final RealDouble weight2 = scores.getScoreWeight(node);

		// Assert:
		MatcherAssert.assertThat(weight2, IsSame.sameInstance(weight1));
	}
}
