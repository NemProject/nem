package org.nem.nis.controller.requests;

import org.nem.core.model.ncc.AccountId;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Model that contains data for requesting historical account data.
 */
public class AccountHistoricalDataRequest extends AccountId {
	private static final long MAX_RANGE = 1000;

	private final BlockHeight startHeight;
	private final BlockHeight endHeight;

	/**
	 * Creates a new account historical data object.
	 *
	 * @param address The account's address.
	 * @param startHeight The start height.
	 * @param endHeight the end height.
	 */
	public AccountHistoricalDataRequest(final String address, final String startHeight, final String endHeight) {
		super(address);
		this.startHeight = new BlockHeight(Long.parseLong(startHeight));
		this.endHeight = new BlockHeight(Long.parseLong(endHeight));
		this.checkRange(this.startHeight, this.endHeight);
	}

	private void checkRange(final BlockHeight startHeight, final BlockHeight endHeight) {
		final long range = endHeight.subtract(startHeight);
		if (0 > range || MAX_RANGE < range) {
			throw new IllegalArgumentException("start and end height are out of valid range");
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
}
