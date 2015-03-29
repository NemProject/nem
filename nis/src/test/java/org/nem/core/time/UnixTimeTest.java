package org.nem.core.time;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class UnixTimeTest {
	private static final long EPOCH_TIME = 1427587585000L;

	@Test
	public void unixTimeCanBeCreatedAroundTimeInstant() {
		// Act:
		final UnixTime unixTime = UnixTime.fromTimeInstant(new TimeInstant(65));

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(65)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 65000));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-03-29 00:07:30"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundDateString() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("2015-04-19 11:39:47", TimeInstant.ZERO);

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 1856002L * 1000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:39:47"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundSystemTime() {
		// Act:
		final UnixTime unixTime = UnixTime.fromUnixTimeInMillis(EPOCH_TIME + 1856002L * 1000L);

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 1856002L * 1000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:39:47"));
	}

	@Test
	public void fromDateStringReturnsDefaultTimeIfTimeStringCannotBeParsed() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("XYZ", new TimeInstant(1856002));

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(EPOCH_TIME + 1856002L * 1000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:39:47"));
	}
}