package org.nem.core.time;

/**
 * Interface that provides time-related information.
 */
public interface TimeProvider {

    /**
     * Gets the epoch time.
     *
     * @return The epoch time.
     */
    public TimeInstant getEpochTime();

    /**
     * Gets the current time.
     *
     * @return The current time.
     */
    public TimeInstant getCurrentTime();
}
