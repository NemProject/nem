package org.nem.core.time;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;

public class SystemTimeProviderTest {

	@Test
	public void getEpochTimeReturnsZero() {
		// Arrange:
		final TimeProvider provider = new SystemTimeProvider();

		// Assert:
		Assert.assertThat(provider.getEpochTime(), IsEqual.equalTo(TimeInstant.ZERO));
	}

	@Test
	public void getCurrentTimeIsPositive() {
		// Arrange:
		final TimeProvider provider = new SystemTimeProvider();

		// Assert:
		Assert.assertThat(provider.getCurrentTime().compareTo(TimeInstant.ZERO), IsEqual.equalTo(1));
	}

	@Test
	public void getCurrentTimeReturnsExpectedTime() {
		// Act:
		final CurrentTimeInfo ctInfo = getDeterministicCurrentTime();

		// Assert:
		final int expectedTime = (int)((ctInfo.systemTime - SystemTimeProvider.getEpochTimeMillis() + 500L) / 1000);
		Assert.assertThat(ctInfo.currentTime, IsEqual.equalTo(expectedTime));
	}

	@Test
	public void getCurrentTimeIsConsistentWithSystemTime() {
		// Act:
		final CurrentTimeInfo ctInfo = getDeterministicCurrentTime();

		// Assert:
		Assert.assertThat(ctInfo.currentTime, IsEqual.equalTo(ctInfo.currentTimeFromSystemTime));
	}

	@Test
	public void getTimeRoundsTimeToNearestSecond() {
		// Assert:
		Assert.assertThat(getTimeRelativeToEpoch(1000), IsEqual.equalTo(1));
		Assert.assertThat(getTimeRelativeToEpoch(1001), IsEqual.equalTo(1));
		Assert.assertThat(getTimeRelativeToEpoch(1499), IsEqual.equalTo(1));
		Assert.assertThat(getTimeRelativeToEpoch(1500), IsEqual.equalTo(2));
		Assert.assertThat(getTimeRelativeToEpoch(1999), IsEqual.equalTo(2));
		Assert.assertThat(getTimeRelativeToEpoch(2000), IsEqual.equalTo(2));
	}

	@Test
	public void updateTimeOffsetAddsOffset() {
		// Arrange:
		final SystemTimeProvider provider = new SystemTimeProvider();

		// Act:
		provider.updateTimeOffset(new TimeOffset(123));
		provider.updateTimeOffset(new TimeOffset(234));
		final TimeOffset offset = provider.getTimeOffset();

		// Assert:
		Assert.assertThat(offset, IsEqual.equalTo(new TimeOffset(123 + 234)));
	}

	@Test
	public void getNetworkTimeReturnsExpectedTime() {
		// Arrange:
		final SystemTimeProvider provider = new SystemTimeProvider();
		provider.updateTimeOffset(new TimeOffset(123));

		// Act:
		final long curMillis = System.currentTimeMillis() - SystemTimeProvider.getEpochTimeMillis();
		final NetworkTimeStamp nts = provider.getNetworkTime();

		// TODO BR: is there another way to test this?
		// Assert:
		Assert.assertThat(nts.getRaw() < curMillis + 123 + 3, IsEqual.equalTo(true));
		Assert.assertThat(nts.getRaw() > curMillis + 122, IsEqual.equalTo(true));
	}

	private static int getTimeRelativeToEpoch(final int millis) {
		return SystemTimeProvider.getTime(SystemTimeProvider.getEpochTimeMillis() + millis);
	}

	private static class CurrentTimeInfo {
		public int currentTime;
		public int currentTimeFromSystemTime;
		public long systemTime;
	}

	private static CurrentTimeInfo getDeterministicCurrentTime() {
		// Arrange:
		final TimeProvider provider = new SystemTimeProvider();

		// Act:
		final CurrentTimeInfo ctInfo = new CurrentTimeInfo();
		long systemTimeEnd;
		do {
			ctInfo.systemTime = System.currentTimeMillis();

			ctInfo.currentTime = provider.getCurrentTime().getRawTime();
			ctInfo.currentTimeFromSystemTime = SystemTimeProvider.getTime(ctInfo.systemTime);

			systemTimeEnd = System.currentTimeMillis();
		} while (ctInfo.systemTime != systemTimeEnd);

		return ctInfo;
	}
}
