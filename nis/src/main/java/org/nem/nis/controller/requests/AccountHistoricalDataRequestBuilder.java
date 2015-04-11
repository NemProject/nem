package org.nem.nis.controller.requests;

/**
 * Builder that is used by Spring to create an AccountHistoricalDataRequest from a GET request.
 */
public class AccountHistoricalDataRequestBuilder {
	private String address;
	private String startHeight;
	private String endHeight;
	private String increment;

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
	 * Sets the increment.
	 *
	 * @param increment The increment.
	 */
	public void setIncrement(final String increment) {
		this.increment = increment;
	}

	/**
	 * Creates an AccountHistoricalDataRequest.
	 *
	 * @return The account historical data request.
	 */
	public AccountHistoricalDataRequest build() {
		return new AccountHistoricalDataRequest(this.address, this.startHeight, this.endHeight, this.increment);
	}
}
