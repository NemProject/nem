package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountHistoricalDataRequest from a GET request.
 */
public class AccountHistoricalDataRequestBuilder {
	private String address;
	private String startHeight;
	private String endHeight;

	/**
	 * Sets the address.
	 *
	 * @param address The address.
	 */
	public void setAddress(final String address) {
		this.address = address;
	}

	/**
	 * Sets the start height.
	 *
	 * @param startHeight The start height.
	 */
	public void setStartHeight(final String startHeight) {
		this.startHeight = startHeight;
	}

	/**
	 * Sets the end height.
	 *
	 * @param endHeight The end height.
	 */
	public void setEndHeight(final String endHeight) {
		this.endHeight = endHeight;
	}

	/**
	 * Creates an AccountHistoricalDataRequest.
	 *
	 * @return The account historical data request.
	 */
	public AccountHistoricalDataRequest build() {
		return new AccountHistoricalDataRequest(this.address, this.startHeight, this.endHeight);
	}
}
