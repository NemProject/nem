package org.nem.peer.trust.score;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.node.*;

public class TrustScoresTest {

	//region basic operations

	@Test
	public void previouslyUnknownWeightCanBeRetrieved() {
		// Arrange:
		final Node node = NodeUtils.createNodeWithName("bob");
		final TrustScores scores = new TrustScores();

		// Act:
		final RealDouble weight = scores.getScoreWeight(node);

		// Assert:
		Assert.assertThat(weight.get(), IsEqual.equalTo(0.0));
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
		Assert.assertThat(weight2, IsSame.sameInstance(weight1));
	}
}
