package org.nem.core.model;

import java.security.InvalidParameterException;

/**
 * Represents an amount of NEM.
 */
public class Amount implements Comparable<Amount> {

    private final long amount;

    /**
     * Amount representing 0 NEM.
     */
    public static final Amount ZERO = new Amount(0);

    /**
     * Creates a NEM amount.
     *
     * @param amount The number of micro NEM.
     */
    public Amount(long amount) {
        if (amount < 0)
            throw new InvalidParameterException("amount must be non-negative");

        this.amount = amount;
    }

    /**
     * Compares this amount to another Amount.
     *
     * @param rhs The amount to compare against.
     * @return -1, 0 or 1 as this Amount is numerically less than, equal to, or greater than rhs.
     */
    @Override
    public int compareTo(final Amount rhs) {
        return Long.compare(this.amount, rhs.amount);
    }

    /**
     * Returns the number of micro NEM.
     *
     * @return The number of micro NEM.
     */
    public long getNumMicroNem() { return this.amount; }

    @Override
    public int hashCode() {
        return Long.valueOf(this.amount).intValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Amount))
            return false;

        Amount rhs = (Amount)obj;
        return this.amount == rhs.amount;
    }

    @Override
    public String toString() {
        return String.format("%d", this.amount);
    }
}
