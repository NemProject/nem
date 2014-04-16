package org.nem.core.model;

import java.security.InvalidParameterException;

/**
 * Represents a NEM block height.
 */
public class BlockHeight implements Comparable<BlockHeight> {

	/**
	 * Value representing 0 height.
	 */
	public static final BlockHeight ZERO = new BlockHeight(0);

	private final long height;

	/**
	 * Creates a block height.
	 *
	 * @param height The block height.
	 */
	public BlockHeight(long height) {
		if (height < 0)
			throw new InvalidParameterException("height must be non-negative");

		this.height = height;
	}

	/**
	 * Creates a new BlockHeight by adding one to this height.
	 *
	 * @return The new height.
	 */
	public BlockHeight next() {
		return new BlockHeight(this.height + 1);
	}

	/**
	 * Compares this height to another BlockHeight.
	 *
	 * @param rhs The height to compare against.
	 * @return -1, 0 or 1 as this BlockHeight is numerically less than, equal to, or greater than rhs.
	 */
	@Override
	public int compareTo(final BlockHeight rhs) {
		//noinspection SuspiciousNameCombination
		return Long.compare(this.height, rhs.height);
	}

	/**
	 * Returns the underlying height.
	 *
	 * @return The underlying height.
	 */
	public long getRaw() { return this.height; }

	@Override
	public int hashCode() {
		return Long.valueOf(this.height).intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof BlockHeight))
			return false;

		final BlockHeight rhs = (BlockHeight)obj;
		return this.height == rhs.height;
	}

	@Override
	public String toString() {
		return String.format("%d", this.height);
	}
}
