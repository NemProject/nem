package org.nem.core.time;

import java.util.*;

/**
 * Time provider that uses the System time.
 */
public class SystemTimeProvider implements TimeProvider {

    private static final long EPOCH_TIME;
    private static final long EPOCH_TIME_PLUS_ROUNDING;

    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.ERA, 0);
        calendar.set(Calendar.YEAR, 2014);
        calendar.set(Calendar.MONTH, 3);
        calendar.set(Calendar.DAY_OF_MONTH, 9);
        calendar.set(Calendar.HOUR, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH_TIME = calendar.getTimeInMillis();
        EPOCH_TIME_PLUS_ROUNDING = EPOCH_TIME - 500L;
    }

    @Override
    public int getEpochTime() {
        return 0;
    }

    @Override
    public int getCurrentTime() {
        long time = System.currentTimeMillis();
        return getTime(time);
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
     * @return The normalized time.
     */
    public static int getTime(long millis) {
        return (int)((millis - EPOCH_TIME_PLUS_ROUNDING) / 1000L);
    }
}
