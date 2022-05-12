package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class CredibilityScoreTest {

	@Test
	public void scoreHasCorrectInitialValue() {
		// Arrange:
		final Score score = new CredibilityScore();

		// Assert:
		MatcherAssert.assertThat(score.score().get(), IsEqual.equalTo(1.0));
	}
}
