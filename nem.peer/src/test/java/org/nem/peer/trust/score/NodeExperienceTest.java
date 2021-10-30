package org.nem.peer.trust.score;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class NodeExperienceTest {

	// region basic operation

	@Test
	public void nodeExperienceCanBeCreatedWithDefaultValues() {
		// Act:
		final NodeExperience experience = new NodeExperience();

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(experience.getLastUpdateTime(), IsEqual.equalTo(TimeInstant.ZERO));
	}

	@Test
	public void nodeExperienceCanBeCreatedWithInitialExperienceValues() {
		// Act:
		final NodeExperience experience = new NodeExperience(5, 18);

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(5L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(18L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(23L));
		MatcherAssert.assertThat(experience.getLastUpdateTime(), IsEqual.equalTo(TimeInstant.ZERO));
	}

	@Test
	public void nodeExperienceCanBeCreatedWithInitialExperienceValuesAndTimeStamp() {
		// Act:
		final NodeExperience experience = new NodeExperience(5, 18, new TimeInstant(123));

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(5L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(18L));
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(23L));
		MatcherAssert.assertThat(experience.getLastUpdateTime(), IsEqual.equalTo(new TimeInstant(123)));
	}

	@Test
	public void totalCallsReturnsTheSumOfSuccessfulAndFailedCalls() {
		// Arrange:
		final NodeExperience experience = new NodeExperience();

		// Act:
		experience.successfulCalls().set(4);
		experience.failedCalls().set(7);

		// Assert:
		MatcherAssert.assertThat(experience.totalCalls(), IsEqual.equalTo(11L));
	}

	@Test
	public void nodeExperienceCanBeRoundTripped() {
		// Arrange:
		final NodeExperience originalExperience = new NodeExperience();
		originalExperience.successfulCalls().set(11L);
		originalExperience.failedCalls().set(3L);

		// Act:
		final NodeExperience experience = new NodeExperience(Utils.roundtripSerializableEntity(originalExperience, null));

		// Assert:
		MatcherAssert.assertThat(experience.successfulCalls().get(), IsEqual.equalTo(11L));
		MatcherAssert.assertThat(experience.failedCalls().get(), IsEqual.equalTo(3L));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeExperience experience = createNodeExperience(3, 15);

		// Assert:
		MatcherAssert.assertThat(createNodeExperience(3, 15), IsEqual.equalTo(experience));
		MatcherAssert.assertThat(createNodeExperience(15, 3), IsNot.not(IsEqual.equalTo(experience)));
		MatcherAssert.assertThat(createNodeExperience(3, 14), IsNot.not(IsEqual.equalTo(experience)));
		MatcherAssert.assertThat(createNodeExperience(4, 15), IsNot.not(IsEqual.equalTo(experience)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(experience)));
		MatcherAssert.assertThat(3L, IsNot.not(IsEqual.equalTo((Object) experience)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeExperience experience = createNodeExperience(3, 15);
		final int hashCode = experience.hashCode();

		// Assert:
		MatcherAssert.assertThat(createNodeExperience(3, 15).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(createNodeExperience(15, 3).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(createNodeExperience(3, 14).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createNodeExperience(4, 15).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateStringRepresentation() {
		// Arrange:
		final NodeExperience experience = createNodeExperience(3, 15);

		// Assert:
		MatcherAssert.assertThat(experience.toString(), IsEqual.equalTo("success: 3, failure: 15"));
	}

	// endregion

	private static NodeExperience createNodeExperience(final int numSuccesses, final int numFailures) {
		return new NodeExperience(numSuccesses, numFailures);
	}
}
