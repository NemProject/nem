package org.nem.nis.controller.requests;

import org.nem.core.model.ncc.AccountId;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Model that contains data for requesting historical account data.
 */
public class AccountHistoricalDataRequest extends AccountId {
	private static final long MAX_DATA_POINTS = 1000;

	private final BlockHeight startHeight;
	private final BlockHeight endHeight;
	private final Long increment;

	/**
	 * Creates a new account historical data object.
	 *
	 * @param address The account's address.
	 * @param startHeight The start height.
	 * @param endHeight The end height.
	 * @param incrementBy The increment by which to increase the height.
	 */
	public AccountHistoricalDataRequest(
			final String address,
			final String startHeight,
			final String endHeight,
			final String incrementBy) {
		super(address);
		this.startHeight = new BlockHeight(Long.parseLong(startHeight));
		this.endHeight = new BlockHeight(Long.parseLong(endHeight));
		this.increment = Long.parseLong(incrementBy);
		this.checkConsistency();
	}

	private void checkConsistency() {
		final long range = this.endHeight.subtract(this.startHeight);
		if (0 > range) {
			throw new IllegalArgumentException("start and end height are out of valid range");
		}

		if (0 >= this.increment) {
			throw new IllegalArgumentException("increment must be a positive integer");
		}

		if (MAX_DATA_POINTS < range / this.increment) {
			throw new IllegalArgumentException(String.format("only up to %d data points are supported", MAX_DATA_POINTS));
		}
	}

	/**
	 * Gets the start height.
	 *
	 * @return The start height.
	 */
	public BlockHeight getStartHeight() {
		return this.startHeight;
	}

	/**
	 * Gets the end height.
	 *
	 * @return The end height.
	 */
	public BlockHeight getEndHeight() {
		return this.endHeight;
	}

	/**
	 * Gets the increment.
	 *
	 * @return The increment.
	 */
	public Long getIncrement() {
		return this.increment;
	}
}
