package org.nem.core.time;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.TimeOffset;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class SystemTimeProviderTest {
	private static final long EPOCH_TIME = 1427587585000L;

	@Before
	public void resetTimeOffset() {
		final SystemTimeProvider provider = new SystemTimeProvider();
		final Field field;
		try {
			field = SystemTimeProvider.class.getDeclaredField("timeOffset");
			field.setAccessible(true);
			field.set(provider, new TimeOffset(0));
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException("Failed to reset time offset");
		}
	}

	//region getEpochTime[Millis]

	@Test
	public void getEpochTimeReturnsZero() {
		// Arrange:
		final TimeProvider provider = new SystemTimeProvider();

		// Assert:
		Assert.assertThat(provider.getEpochTime(), IsEqual.equalTo(TimeInstant.ZERO));
	}

	@Test
	public void getEpochTimeMillisReturnsEpochInMilliseconds() {
		// Assert:
		Assert.assertThat(SystemTimeProvider.getEpochTimeMillis(), IsEqual.equalTo(EPOCH_TIME));
	}

	//endregion

	//region getCurrentTime

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
		Assert.assertThat(ctInfo.currentTime, IsEqual.equalTo(convertSystemTimeToCurrentTime(ctInfo.systemTime, 0)));
	}

	@Test
	public void getCurrentTimeIsConsistentWithSystemTime() {
		// Act:
		final CurrentTimeInfo ctInfo = getDeterministicCurrentTime();

		// Assert:
		Assert.assertThat(ctInfo.currentTime, IsEqual.equalTo(ctInfo.currentTimeFromSystemTime));
	}

	//endregion

	//region getNetworkTime

	@Test
	public void getNetworkTimeIsInitiallySynchronizedWithSystemTime() {
		// Act:
		final CurrentTimeInfo ctInfo = getDeterministicCurrentTime();

		// Assert:
		Assert.assertThat(ctInfo.networkTime, IsEqual.equalTo(convertSystemTimeToNetworkTime(ctInfo.systemTime, 0)));
	}

	//endregion

	//region getTime

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

	private static int getTimeRelativeToEpoch(final int millis) {
		return SystemTimeProvider.getTime(SystemTimeProvider.getEpochTimeMillis() + millis);
	}

	//endregion

	//region updateTimeOffset

	@Test
	public void updateTimeOffsetAdjustsNetworkTime() {
		// Arrange:
		final NetworkTimeInfo info = new NetworkTimeInfo();
		final long systemTime = runDeterministicOperation(v -> {
			final SystemTimeProvider provider = new SystemTimeProvider();

			// Act:
			info.result = provider.updateTimeOffset(new TimeOffset(123000));
			info.currentTime = provider.getCurrentTime();
			info.networkTime = provider.getNetworkTime();

			// because the time offset adjustment is static across instances,
			// we need to keep track of how many times adjustments were made
			++info.rounds;
		});

		// Assert:
		info.assertSystemTimeOffset(systemTime, 123000, 123000 * info.rounds);
	}

	@Test
	public void updateTimeOffsetAdjustsNetworkTimeCumulatively() {
		// Arrange:
		final NetworkTimeInfo info = new NetworkTimeInfo();
		final long systemTime = runDeterministicOperation(v -> {
			final SystemTimeProvider provider = new SystemTimeProvider();

			// Act:
			provider.updateTimeOffset(new TimeOffset(123000));
			provider.updateTimeOffset(new TimeOffset(-23000));
			info.result = provider.updateTimeOffset(new TimeOffset(111111));
			info.currentTime = provider.getCurrentTime();
			info.networkTime = provider.getNetworkTime();

			// because the time offset adjustment is static across instances,
			// we need to keep track of how many times adjustments were made
			++info.rounds;
		});

		// Assert:
		info.assertSystemTimeOffset(systemTime, 111111, 211111 * info.rounds);
	}

	private static class NetworkTimeInfo {
		public TimeSynchronizationResult result;
		public TimeInstant currentTime;
		public NetworkTimeStamp networkTime;
		public int rounds = 0;

		private void assertSystemTimeOffset(final long systemTime, final int offset, final int cumulativeOffset) {
			Assert.assertThat(this.result.getChange(), IsEqual.equalTo(new TimeOffset(offset)));
			Assert.assertThat(this.result.getCurrentTimeOffset(), IsEqual.equalTo(new TimeOffset(cumulativeOffset)));
			Assert.assertThat(this.result.getTimeStamp(), IsEqual.equalTo(convertSystemTimeToCurrentTime(systemTime, cumulativeOffset)));

			Assert.assertThat(this.currentTime, IsEqual.equalTo(convertSystemTimeToCurrentTime(systemTime, cumulativeOffset)));
			Assert.assertThat(this.networkTime, IsEqual.equalTo(convertSystemTimeToNetworkTime(systemTime, cumulativeOffset)));
		}
	}

	//endregion

	//region timeOffset preservation across instances

	@Test
	public void timeOffsetIsPreservedAcrossInstances() {
		// Arrange:
		final TimeProvider[] providers = { new SystemTimeProvider(), new SystemTimeProvider(), null };
		final int numProviders = 3;
		final long[] networkTimes = new long[numProviders];

		// Act:
		runDeterministicOperation(v -> {
			providers[0].updateTimeOffset(new TimeOffset(1234L));
			providers[2] = new SystemTimeProvider();
			for (int i = 0; i < numProviders; i++) {
				networkTimes[i] = providers[i].getNetworkTime().getRaw();
			}
		});

		// Assert:
		Assert.assertThat(networkTimes[0], IsEqual.equalTo(networkTimes[1]));
		Assert.assertThat(networkTimes[0], IsEqual.equalTo(networkTimes[2]));
	}

	//endregion

	private static class CurrentTimeInfo {
		public TimeInstant currentTime;
		public TimeInstant currentTimeFromSystemTime;
		public NetworkTimeStamp networkTime;
		public long systemTime;
	}

	private static TimeInstant convertSystemTimeToCurrentTime(final long systemTime, final long offset) {
		return new TimeInstant((int)((systemTime - SystemTimeProvider.getEpochTimeMillis() + 500L + offset) / 1000));
	}

	private static NetworkTimeStamp convertSystemTimeToNetworkTime(final long systemTime, final long offset) {
		return new NetworkTimeStamp(systemTime - EPOCH_TIME + offset);
	}

	private static CurrentTimeInfo getDeterministicCurrentTime() {
		// Arrange:
		final TimeProvider provider = new SystemTimeProvider();

		// Act:
		final CurrentTimeInfo ctInfo = new CurrentTimeInfo();
		ctInfo.systemTime = runDeterministicOperation(v -> {
			ctInfo.currentTime = provider.getCurrentTime();
			ctInfo.networkTime = provider.getNetworkTime();
			ctInfo.currentTimeFromSystemTime = new TimeInstant(SystemTimeProvider.getTime(System.currentTimeMillis()));
		});

		return ctInfo;
	}

	private static long runDeterministicOperation(final Consumer<Void> operation) {
		long systemTimeStart;
		long systemTimeEnd;
		do {
			systemTimeStart = System.currentTimeMillis();
			operation.accept(null);
			systemTimeEnd = System.currentTimeMillis();
		} while (systemTimeStart != systemTimeEnd);

		return systemTimeEnd;
	}
}
