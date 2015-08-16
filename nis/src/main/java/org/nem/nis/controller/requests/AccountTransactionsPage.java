package org.nem.nis.controller.requests;

import org.nem.core.crypto.Hash;
import org.nem.core.model.ncc.AccountId;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.StringUtils;

public class AccountTransactionsPage extends AccountId {
	private final Hash hash;
	private final Long id;

	/**
	 * Creates a new account page.
	 *
	 * @param address The address.
	 * @param hash The hash.
	 * @param id The id.
	 */
	public AccountTransactionsPage(final String address, final String hash, final String id) {
		super(address);
		this.hash = StringUtils.isNullOrEmpty(hash) ? null : Hash.fromHexString(hash);
		this.id = StringUtils.isNullOrEmpty(id) ? null : Long.parseLong(id);
	}

	/**
	 * Gets the hash.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	public Long getId() {
		return this.id;
	}
}
