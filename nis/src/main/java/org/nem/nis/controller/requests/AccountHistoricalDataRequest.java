package org.nem.nis.controller.requests;

import org.nem.core.model.ncc.AccountId;
import org.nem.core.model.primitive.BlockHeight;

/**
 * Model that contains data for requesting historical account data.
 */
public class AccountHistoricalDataRequest extends AccountId {
	private final HistoricalDataRequest historicalDataRequest;

	/**
	 * Creates a new account historical data object.
	 *
	 * @param address The account's address.
	 * @param startHeight The start height.
	 * @param endHeight The end height.
	 * @param incrementBy The increment by which to increase the height.
	 */
	public AccountHistoricalDataRequest(final String address, final String startHeight, final String endHeight, final String incrementBy) {
		super(address);
		this.historicalDataRequest = new HistoricalDataRequest(new BlockHeight(Long.parseLong(startHeight)),
				new BlockHeight(Long.parseLong(endHeight)), Long.parseLong(incrementBy));
	}

	/**
	 * Gets the start height.
	 *
	 * @return The start height.
	 */
	public BlockHeight getStartHeight() {
		return this.historicalDataRequest.getStartHeight();
	}

	/**
	 * Gets the end height.
	 *
	 * @return The end height.
	 */
	public BlockHeight getEndHeight() {
		return this.historicalDataRequest.getEndHeight();
	}

	/**
	 * Gets the increment.
	 *
	 * @return The increment.
	 */
	public Long getIncrement() {
		return this.historicalDataRequest.getIncrement();
	}
}
