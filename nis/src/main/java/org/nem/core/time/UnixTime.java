package org.nem.core.time;

import java.text.*;
import java.util.*;

/**
 * Helper class for converting TimeInstant to and from Unix time.
 */
public class UnixTime {
	private final TimeInstant timeInstant;

	private UnixTime(final TimeInstant timeInstant) {
		this.timeInstant = timeInstant;
	}

	/**
	 * Creates a unix time given a time instant.
	 *
	 * @param timeInstant The time instant.
	 * @return The unix time.
	 */
	public static UnixTime fromTimeInstant(final TimeInstant timeInstant) {
		return new UnixTime(timeInstant);
	}

	/**
	 * Creates a unix time given the unix time in milliseconds.
	 *
	 * @param millis The unix time in milliseconds.
	 * @return The unix time.
	 */
	public static UnixTime fromUnixTimeInMillis(final long millis) {
		return new UnixTime(new TimeInstant(SystemTimeProvider.getTime(millis)));
	}

	/**
	 * Creates a unix time given a date string.
	 *
	 * @param dateString The date string.
	 * @param defaultValue The default value that should be returned if the date string cannot be parsed.
	 * @return The unix time.
	 */
	public static UnixTime fromDateString(final String dateString, final TimeInstant defaultValue) {
		try {
			final Date date = createDateFormat().parse(dateString);
			return new UnixTime(new TimeInstant(SystemTimeProvider.getTime(date.getTime())));
		} catch (final ParseException e) {
			return new UnixTime(defaultValue);
		}
	}

	/**
	 * Gets the corresponding time instant.
	 *
	 * @return The time instant.
	 */
	public TimeInstant getTimeInstant() {
		return this.timeInstant;
	}

	/**
	 * Gets the unix time in milliseconds.
	 *
	 * @return The unix time.
	 */
	public long getMillis() {
		return SystemTimeProvider.getEpochTimeMillis() + this.timeInstant.getRawTime() * 1000L;
	}

	/**
	 * Gets the unix time as a date string.
	 *
	 * @return The date string.
	 */
	public String getDateString() {
		return createDateFormat().format(new Date(this.getMillis()));
	}

	private static SimpleDateFormat createDateFormat() {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}
}
