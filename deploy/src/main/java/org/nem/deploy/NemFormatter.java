package org.nem.deploy;

import org.nem.core.time.*;

import java.time.Instant;
import java.util.TimeZone;
import java.util.logging.*;

/**
 * Formatter adds network time to logs.
 */
public class NemFormatter extends SimpleFormatter {
	private static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();
	private static final int TIME_ZONE_OFFSET = TimeZone.getDefault().getRawOffset();

	@Override
	public synchronized String format(final LogRecord record) {
		record.setInstant(
				Instant.ofEpochMilli(TIME_PROVIDER.getNetworkTime().getRaw() + SystemTimeProvider.getEpochTimeMillis() - TIME_ZONE_OFFSET));
		return super.format(record);
	}
}
