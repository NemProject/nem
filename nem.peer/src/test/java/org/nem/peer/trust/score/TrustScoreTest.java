package org.nem.peer.trust.score;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class TrustScoreTest {

	@Test
	public void scoreHasCorrectInitialValue() {
		// Arrange:
		final Score score = new TrustScore();

		// Assert:
		Assert.assertThat(score.score().get(), IsEqual.equalTo(0.0));
	}
}
