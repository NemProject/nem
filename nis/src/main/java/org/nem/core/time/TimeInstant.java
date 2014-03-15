package org.nem.core.time;

import java.security.InvalidParameterException;

/**
 * Represents an instant in time.
 */
public class TimeInstant {

    private final int time;

    /**
     * Creates an instant in time.
     *
     * @param time The number of seconds passed since the epoch.
     */
    public TimeInstant(int time) {
        if (time < 0)
            throw new InvalidParameterException("time must be non-negative");

        this.time = time;
    }

    /**
     * Creates a new TimeInstant by adding the specified number of seconds to this instant.
     *
     * @param seconds The number of seconds to add.
     * @return The new instant.
     */
    public TimeInstant addSeconds(final int seconds) {
        return new TimeInstant(this.time + seconds);
    }

    /**
     * Creates a new TimeInstant by adding the specified number of minutes to this instant.
     *
     * @param minutes The number of minutes to add.
     * @return The new instant.
     */
    public TimeInstant addMinutes(final int minutes) {
        return this.addSeconds(60 * minutes);
    }

    /**
     * Creates a new TimeInstant by adding the specified number of hours to this instant.
     *
     * @param hours The number of hours to add.
     * @return The new instant.
     */
    public TimeInstant addHours(final int hours) {
        return this.addMinutes(60 * hours);
    }

    /**
     * Creates a new TimeInstant by adding the specified number of days to this instant.
     *
     * @param hours The number of days to add.
     * @return The new instant.
     */
    public TimeInstant addDays(final int hours) {
        return this.addHours(24 * hours);
    }

    /**
     * Returns the number of seconds between this TimeInstant and rhs.
     *
     * @param rhs The value to subtract.
     * @return The subtraction result.
     */
    public int subtract(final TimeInstant rhs) {
        return this.time - rhs.time;
    }

    /**
     * Compares this instant to another TimeInstant.
     *
     * @param rhs The instant to compare against.
     * @return -1, 0 or 1 as this TimeInstant is numerically less than, equal to, or greater than rhs.
     */
    public int compareTo(final TimeInstant rhs) {
        if (this.time == rhs.time)
            return 0;

        return this.time < rhs.time ? -1 : 1;
    }

    /**
     * Returns the number of seconds passed since the epoch.
     *
     * @return The number of seconds passed since the epoch.
     */
    public int getRawTime() { return this.time; }

    @Override
    public int hashCode() {
        return this.time;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TimeInstant))
            return false;

        TimeInstant rhs = (TimeInstant)obj;
        return this.time == rhs.time;
    }

    @Override
    public String toString() {
        return String.format("%d", this.time);
    }
}
