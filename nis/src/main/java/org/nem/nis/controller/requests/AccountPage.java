package org.nem.nis.controller.requests;

import org.nem.core.model.ncc.AccountId;

/**
 * View model that represents a page of account-related information.
 */
public class AccountPage extends AccountId {
	private final String timeStamp;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param timeStamp The timestamp.
	 */
	public AccountPage(final String address, final String timeStamp) {
		super(address);
		this.timeStamp = timeStamp;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return The timestamp.
	 */
	public String getTimeStamp() {
		return this.timeStamp;
	}
}
