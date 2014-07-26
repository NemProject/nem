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
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2014-06-04 00:01:05"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundDateString() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("2014-06-25 11:33:22", TimeInstant.ZERO);

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(1403696002000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2014-06-25 11:33:22"));
	}

	@Test
	public void unixTimeCanBeCreatedAroundSystemTime() {
		// Act:
		final UnixTime unixTime = UnixTime.fromUnixTimeInMillis(1406288002000L);

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(4448002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(1406288002000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2014-07-25 11:33:22"));
	}

	@Test
	public void fromDateStringReturnsDefaultTimeIfTimeStringCannotBeParsed() {
		// Act:
		final UnixTime unixTime = UnixTime.fromDateString("XYZ", new TimeInstant(1856002));

		// Assert:
		Assert.assertThat(unixTime.getTimeInstant(), IsEqual.equalTo(new TimeInstant(1856002)));
		Assert.assertThat(unixTime.getMillis(), IsEqual.equalTo(1403696002000L));
		Assert.assertThat(unixTime.getDateString(), IsEqual.equalTo("2014-06-25 11:33:22"));
	}
}