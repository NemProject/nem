package org.nem.core.time;

import org.hamcrest.core.IsEqual;
import org.junit.*;

public class UnixTimeTest {

	@Test
	public void unixTimeCanBeCreatedAroundTimeInstant() {
		// Act:
		final UnixTime unixTime = UnixTime.fromTimeInstant(new TimeInstant(65));

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(65)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(SystemTimeProvider.getEpochTimeMillis() + 65000));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-03-29 00:01:05"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundDateString() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("2015-04-19 11:33:22", TimeInstant.ZERO);

		// Assert:
		// TODO: for some reason results in this test differs by a second when compared
		// to fromDateStringReturnsDefaultTimeIfTimeStringCannotBeParsed
		// probably due to non-zero milliseconds in SystemTimeProvider....
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(1427587200625L + 1856002L * 1000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:33:22"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundSystemTime() {
		// Act:
		final UnixTime unixTime = UnixTime.fromUnixTimeInMillis(1427587200625L + 1856002L * 1000L);

		// Assert:
		// TODO: as above
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(1427587200625L + 1856002L * 1000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-03-19 11:33:22"));
	}

	@Test
	public void fromDateStringReturnsDefaultTimeIfTimeStringCannotBeParsed() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("XYZ", new TimeInstant(1856002));

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(1427587200625L + 1856002L * 1000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2015-04-19 11:33:22"));
	}
}