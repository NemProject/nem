package org.nem.core.time;

import org.hamcrest.core.*;
import org.junit.*;

public class SystemTimeProviderTest {

    @Test
    public void getEpochTimeReturnsZero() {
        // Arrange:
        TimeProvider provider = new SystemTimeProvider();

        // Assert:
        Assert.assertThat(provider.getEpochTime(), IsEqual.equalTo(new TimeInstant(0)));
    }

	@Test
	public void getCurrentTimeIsPositive() {
		// Arrange:
		TimeProvider provider = new SystemTimeProvider();

		// Assert:
		Assert.assertThat(provider.getCurrentTime().compareTo(new TimeInstant(0)), IsEqual.equalTo(1));
	}

    @Test
    public void getCurrentTimeReturnsExpectedTime() {
        // Act:
        CurrentTimeInfo ctInfo = getDeterministicCurrentTime();

        // Assert:
        int expectedTime = (int)((ctInfo.systemTime - SystemTimeProvider.getEpochTimeMillis() + 500L)/1000);
        Assert.assertThat(ctInfo.currentTime, IsEqual.equalTo(expectedTime));
    }

    @Test
    public void getCurrentTimeIsConsistentWithSystemTime() {
        // Act:
        CurrentTimeInfo ctInfo = getDeterministicCurrentTime();

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

    private static int getTimeRelativeToEpoch(int millis) {
        return SystemTimeProvider.getTime(SystemTimeProvider.getEpochTimeMillis() + millis);
    }

    private static class CurrentTimeInfo {
        public int currentTime;
        public int currentTimeFromSystemTime;
        public long systemTime;
    }

    private static CurrentTimeInfo getDeterministicCurrentTime() {
        // Arrange:
        TimeProvider provider = new SystemTimeProvider();

        // Act:
        CurrentTimeInfo ctInfo = new CurrentTimeInfo();
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
