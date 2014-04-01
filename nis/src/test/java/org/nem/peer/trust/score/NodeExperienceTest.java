package org.nem.peer.trust.score;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;

public class NodeExperienceTest {

	@Test
	public void nodeExperienceIsInitializedCorrectly() {
		// Act:
		final NodeExperience experience = new NodeExperience();

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
	}

	@Test
	public void totalCallsReturnsTheSumOfSuccessfulAndFailedCalls() {
		// Arrange:
		final NodeExperience experience = new NodeExperience();

		// Act:
		experience.successfulCalls().set(4);
		experience.failedCalls().set(7);

		// Assert:
		Assert.assertThat(experience.totalCalls(), IsEqual.equalTo(11L));
	}

	@Test
	public void nodeExperienceCanBeRoundTripped() throws Exception {
		// Arrange:
		final NodeExperience originalExperience = new NodeExperience();
		originalExperience.successfulCalls().set(11L);
		originalExperience.failedCalls().set(3L);

		// Act:
		final NodeExperience experience = new NodeExperience(Utils.roundtripSerializableEntity(originalExperience, null));

		// Assert:
		Assert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(11L));
		Assert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(3L));
	}
}
