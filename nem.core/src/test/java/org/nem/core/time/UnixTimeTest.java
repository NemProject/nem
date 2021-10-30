package org.nem.core.time;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class UnixTimeTest {
	private static final long EPOCH_TIME = 1427587585000L;

	@Test
	public void unixTimeCanBeCreatedAroundTimeInstant() {
		// Act:
		final UnixTime unixTime = UnixTime.fromTimeInstant(new TimeInstant(65));

		// Assert:
		MatcherAssert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(65)));
		MatcherAssert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 65000));
		MatcherAssert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-03-29 00:07:30"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundDateString() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("2015-04-19 11:39:47", TimeInstant.ZERO);

		// Assert:
		MatcherAssert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		MatcherAssert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 1856002L * 1000L));
		MatcherAssert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:39:47"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundSystemTime() {
		// Act:
		final UnixTime unixTime = UnixTime.fromUnixTimeInMillis(EPOCH_TIME + 1856002L * 1000L);

		// Assert:
		MatcherAssert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		MatcherAssert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 1856002L * 1000L));
		MatcherAssert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:39:47"));
	}

	@Test
	public void fromDateStringReturnsDefaultTimeIfTimeStringCannotBeParsed() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("XYZ", new TimeInstant(1856002));

		// Assert:
		MatcherAssert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		MatcherAssert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 1856002L * 1000L));
		MatcherAssert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:39:47"));
	}
}
