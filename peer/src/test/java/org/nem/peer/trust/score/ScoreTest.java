package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.peer.test.MockScore;

public class ScoreTest {

	@Test
	public void derivedScoreProvidesInitialValue() {
		// Arrange:
		final Score score = new MockScore();

		// Assert:
		MatcherAssert.assertThat(score.score().get(), IsEqual.equalTo(MockScore.INITIAL_SCORE));
	}

	@Test
	public void rawScoreCanBeChanged() {
		// Arrange:
		final Score score = new MockScore();

		// Act:
		score.score().set(5.2);

		// Assert:
		MatcherAssert.assertThat(score.score().get(), IsEqual.equalTo(5.2));
	}
}
