package org.nem.core.time;

import org.nem.core.model.primitive.*;
import org.nem.nis.controller.viewmodels.TimeSynchronizationResult;

import java.util.*;

/**
 * Time provider that uses the System time.
 */
public class SystemTimeProvider implements TimeProvider {

	private static final long EPOCH_TIME;
	private static final long EPOCH_TIME_PLUS_ROUNDING;
	private TimeOffset timeOffset = new TimeOffset(0);

	static {
		final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.ERA, GregorianCalendar.AD);
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, Calendar.AUGUST);
		calendar.set(Calendar.DAY_OF_MONTH, 4);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		EPOCH_TIME = calendar.getTimeInMillis();
		EPOCH_TIME_PLUS_ROUNDING = EPOCH_TIME - 500L;
	}

	@Override
	public TimeInstant getEpochTime() {
		return TimeInstant.ZERO;
	}

	@Override
	public TimeInstant getCurrentTime() {
		final long time = System.currentTimeMillis() + this.timeOffset.getRaw();
		return new TimeInstant(getTime(time));
	}

	@Override
	public NetworkTimeStamp getNetworkTime() {
		return new NetworkTimeStamp(System.currentTimeMillis() - EPOCH_TIME + this.timeOffset.getRaw());
	}

	@Override
	public TimeSynchronizationResult updateTimeOffset(final TimeOffset offset) {
		this.timeOffset = this.timeOffset.add(offset);
		return new TimeSynchronizationResult(this.getCurrentTime(), this.timeOffset, offset);
	}

	/**
	 * Returns the epoch time in milliseconds.
	 *
	 * @return The epoch time in milliseconds.
	 */
	public static long getEpochTimeMillis() {
		return EPOCH_TIME;
	}

	/**
	 * Returns the normalized time for the specified time.
	 *
	 * @param millis The system time in milliseconds.
	 * @return The normalized time in seconds.
	 */
	public static int getTime(final long millis) {
		return (int)((millis - EPOCH_TIME_PLUS_ROUNDING) / 1000L);
	}
}
