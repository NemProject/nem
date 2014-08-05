package org.nem.peer.trust.score;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class CredibilityScoreTest {

	@Test
	public void scoreHasCorrectInitialValue() {
		// Arrange:
		final Score score = new CredibilityScore();

		// Assert:
		Assert.assertThat(score.score().get(), IsEqual.equalTo(1.0));
	}
}
