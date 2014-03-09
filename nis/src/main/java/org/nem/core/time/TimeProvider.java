package org.nem.core.time;

/**
 * Interface that provides time-related information.
 */
public interface TimeProvider {

    /**
     * Gets the epoch time.
     */
    public int getEpochTime();

    /**
     * Gets the current time.
     */
    public int getCurrentTime();
}
