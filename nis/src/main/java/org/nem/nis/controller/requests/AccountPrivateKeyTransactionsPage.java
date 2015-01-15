package org.nem.nis.controller.requests;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.StringUtils;

/**
 * Class holding information to fetch a batch of transactions and decode the messages.
 */
public class AccountPrivateKeyTransactionsPage {
	private final Hash hash;
	private final Long id;
	private final PrivateKey privateKey;

	/**
	 * Creates an AccountPrivateKeyTransactionsPage.
	 *
	 * @param privateKey The private key.
	 * @param hash The (optional) hash.
	 * @param id The (optional) id.
	 */
	public AccountPrivateKeyTransactionsPage(
			final PrivateKey privateKey,
			final String hash,
			final String id) {
		if (null == privateKey) {
			throw new IllegalArgumentException("private key must not be null.");
		}

		this.privateKey = privateKey;
		this.hash = StringUtils.isNullOrEmpty(hash) ? null : Hash.fromHexString(hash);
		this.id = StringUtils.isNullOrEmpty(id) ? null : Long.parseLong(id);
	}

	/**
	 * Creates an AccountPrivateKeyTransactionsPage.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public AccountPrivateKeyTransactionsPage(final Deserializer deserializer) {
		this(
				new PrivateKey(deserializer),
				deserializer.readOptionalString("hash"),
				deserializer.readOptionalString("id"));
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
	 * Creates an AccountTransactionsPageBuilder from the page.
	 *
	 * @return The AccountTransactionsPageBuilder.
	 */
	public AccountTransactionsPageBuilder createPageBuilder() {
		final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
		pageBuilder.setAddress(Address.fromPublicKey(new KeyPair(this.privateKey).getPublicKey()).getEncoded());
		pageBuilder.setHash(null == this.getHash()? null : this.getHash().toString());
		pageBuilder.setId(null == this.getId()? null : this.getId().toString());
		return pageBuilder;
	}
}
