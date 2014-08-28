package org.nem.core.time;

import java.util.*;

/**
 * Time provider that uses the System time.
 */
public class SystemTimeProvider implements TimeProvider {

	private static final long EPOCH_TIME;
	private static final long EPOCH_TIME_PLUS_ROUNDING;

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
		final long time = System.currentTimeMillis();
		return new TimeInstant(getTime(time));
	}

	/**
	 * Returns the current time in milliseconds.
	 * TODO-CR J-B: if you are using this (it doesn't look like it is being called) consider adding a test for it
	 * TODO-CR J-B: but this seems to be a more precise version of getCurrentTime, so i'm not sure if you are
	 * TODO    Br -> J This is unfinished, there probably will be getNetworkTime and getNetworkTimeMillis too.
	 * TODO            I will add tests once I get to this point.
	 *
	 * @return The current time in milliseconds.
	 */
	public static long getCurrentTimeMillis() {
		return System.currentTimeMillis() - EPOCH_TIME;
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
