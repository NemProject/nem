package org.nem.nis.controller.requests;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.StringUtils;

/**
 * Class holding information to fetch a batch of transactions and decode the messages.
 */
public class AccountPrivateKeyTransactionsPage {
	private final PrivateKey privateKey;
	private final Hash hash;
	private final Long id;
	private final Integer pageSize;

	/**
	 * Creates an account private key transactions page.
	 *
	 * @param privateKey The private key.
	 */
	public AccountPrivateKeyTransactionsPage(final PrivateKey privateKey) {
		this(privateKey, null, null, null);
	}

	/**
	 * Creates an account private key transactions page.
	 *
	 * @param privateKey The private key.
	 * @param hash The (optional) hash.
	 * @param id The (optional) id.
	 * @param pageSize The (optional) page size.
	 */
	public AccountPrivateKeyTransactionsPage(final PrivateKey privateKey, final String hash, final String id, final String pageSize) {
		if (null == privateKey) {
			throw new IllegalArgumentException("private key must not be null.");
		}

		this.privateKey = privateKey;
		this.hash = StringUtils.isNullOrEmpty(hash) ? null : Hash.fromHexString(hash);
		this.id = StringUtils.isNullOrEmpty(id) ? null : Long.parseLong(id);
		this.pageSize = StringUtils.isNullOrEmpty(pageSize) ? null : Integer.parseInt(pageSize);
	}

	/**
	 * Deserializes an account private key transactions page.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public AccountPrivateKeyTransactionsPage(final Deserializer deserializer) {
		this(new PrivateKey(deserializer), deserializer.readOptionalString("hash"), deserializer.readOptionalString("id"),
				deserializer.readOptionalString("pageSize"));
	}

	/**
	 * Gets the private key.
	 *
	 * @return The private key.
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	/**
	 * Gets the (optional) hash.
	 *
	 * @return The hash.
	 */
	public Hash getHash() {
		return this.hash;
	}

	/**
	 * Gets the (optional) id.
	 *
	 * @return The id.
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Gets the (optional) page size.
	 *
	 * @return The page size.
	 */
	public Integer getPageSize() {
		return this.pageSize;
	}

	/**
	 * Creates an AccountTransactionsIdBuilder from the page.
	 *
	 * @return The AccountTransactionsIdBuilder.
	 */
	public AccountTransactionsIdBuilder createIdBuilder() {
		final AccountTransactionsIdBuilder idBuilder = new AccountTransactionsIdBuilder();
		idBuilder.setAddress(Address.fromPublicKey(new KeyPair(this.privateKey).getPublicKey()).getEncoded());
		idBuilder.setHash(null == this.getHash() ? null : this.getHash().toString());
		return idBuilder;
	}

	/**
	 * Creates a DefaultPageBuilder from the page.
	 *
	 * @return The DefaultPageBuilder.
	 */
	public DefaultPageBuilder createPageBuilder() {
		final DefaultPageBuilder pageBuilder = new DefaultPageBuilder();
		pageBuilder.setId(null == this.getId() ? null : this.getId().toString());
		pageBuilder.setPageSize(null == this.getPageSize() ? null : this.getPageSize().toString());
		return pageBuilder;
	}
}
