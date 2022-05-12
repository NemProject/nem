package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class TrustScoreTest {

	@Test
	public void scoreHasCorrectInitialValue() {
		// Arrange:
		final Score score = new TrustScore();

		// Assert:
		MatcherAssert.assertThat(score.score().get(), IsEqual.equalTo(0.0));
	}
}
