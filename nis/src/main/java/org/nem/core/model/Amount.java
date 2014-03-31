package org.nem.core.model;

import java.security.InvalidParameterException;

/**
 * Represents an amount of NEM.
 */
public class Amount implements Comparable<Amount> {

	public static final int MICRONEMS_IN_NEM = 1000000;
	private final long amount;

    /**
     * Amount representing 0 NEM.
     */
    public static final Amount ZERO = new Amount(0);

    /**
     * Creates a new amount given a quantity of NEM.
     *
     * @param amount The amount of NEM.
     * @return The new amount.
     */
	public static Amount fromNem(long amount) {
		return new Amount(amount * MICRONEMS_IN_NEM);
	}

    /**
     * Creates a new amount given a quantity of micro NEM.
     *
     * @param amount The amount of micro NEM.
     * @return The new amount.
     */
    public static Amount fromMicroNem(long amount) {
        return new Amount(amount);
    }

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
     * Creates a new Amount by adding the specified amount to this amount.
     *
     * @param amount The specified amount.
     * @return The new amount.
     */
    public Amount add(final Amount amount) {
        return new Amount(this.getNumMicroNem() + amount.getNumMicroNem());
    }

    /**
     * Creates a new Amount by subtracting the specified amount from this amount.
     *
     * @param amount The specified amount.
     * @return The new amount.
     */
    public Amount subtract(final Amount amount) {
        return new Amount(this.getNumMicroNem() - amount.getNumMicroNem());
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

	/**
	 * Returns the number of NEM (rounded down to the nearest NEM).
	 *
	 * @return The number of NEM.
	 */
	public long getNumNem() { return this.amount / MICRONEMS_IN_NEM; }

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
