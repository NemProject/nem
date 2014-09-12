package org.nem.nis.controller.requests;

import org.nem.core.crypto.Hash;
import org.nem.core.utils.StringUtils;

public class AccountTransactionsPage extends AccountId {
	private final Hash hash;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param hash The hash.
	 */
	public AccountTransactionsPage(final String address, final String hash) {
		super(address);
		this.hash = StringUtils.isNullOrEmpty(hash) ? null : Hash.fromHexString(hash);
	}

	/**
	 * Gets the hash.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}
}
