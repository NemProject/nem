package org.nem.core.time;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.TimeOffset;
import org.nem.core.test.Utils;

public class TimeSynchronizationResultTest {

	// region constructor

	@Test
	public void canCreateTimeSynchronizationResult() {
		// Act:
		final TimeSynchronizationResult result = new TimeSynchronizationResult(new TimeInstant(50), new TimeOffset(17), new TimeOffset(7));

		// Assert:
		MatcherAssert.assertThat(result.getTimeStamp(), IsEqual.equalTo(new TimeInstant(50)));
		MatcherAssert.assertThat(result.getCurrentTimeOffset(), IsEqual.equalTo(new TimeOffset(17)));
		MatcherAssert.assertThat(result.getChange(), IsEqual.equalTo(new TimeOffset(7)));
	}

	// endregion

	// region serialization

	@Test
	public void timeSynchronizationResultCanBeRoundTripped() {
		// Arrange:
		final TimeSynchronizationResult originalResult = new TimeSynchronizationResult(new TimeInstant(50), new TimeOffset(17),
				new TimeOffset(7));

		// Act:
		final TimeSynchronizationResult result = new TimeSynchronizationResult(Utils.roundtripSerializableEntity(originalResult, null));

		// Assert:
		MatcherAssert.assertThat(result.getTimeStamp(), IsEqual.equalTo(new TimeInstant(50)));
		MatcherAssert.assertThat(result.getCurrentTimeOffset(), IsEqual.equalTo(new TimeOffset(17)));
		MatcherAssert.assertThat(result.getChange(), IsEqual.equalTo(new TimeOffset(7)));
	}

	// endregion
}
