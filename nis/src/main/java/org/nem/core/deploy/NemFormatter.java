package org.nem.core.deploy;

import org.nem.core.time.*;

import java.util.TimeZone;
import java.util.logging.*;

/**
 * Formatter which formats log entries.
 */
public class NemFormatter extends SimpleFormatter {
	private static final TimeProvider timeProvider = new SystemTimeProvider();
	private static final int timeZoneOffset = TimeZone.getDefault().getRawOffset();

	@Override
	public synchronized String format(final LogRecord record) {
		final UnixTime time = UnixTime.fromTimeInstant(timeProvider.getCurrentTime());
		record.setMillis(time.getMillis() - timeZoneOffset);
		return super.format(record);
	}
}
