package org.nem.nis.controller.requests;

import org.nem.core.crypto.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;

/**
 * Class holding information to fetch a batch of transactions and decode the messages.
 */
public class AccountTransactionsPagePrivateKeyPair implements SerializableEntity {
	private final AccountTransactionsPage page;
	private final PrivateKey privateKey;

	/**
	 * Creates an AccountTransactionsPagePrivateKeyPair.
	 *
	 * @param page The account transactions page.
	 * @param privateKey The private key.
	 */
	public AccountTransactionsPagePrivateKeyPair(
			final AccountTransactionsPage page,
			final PrivateKey privateKey) {
		this.page = page;
		this.privateKey = privateKey;
	}

	/**
	 * Creates an AccountTransactionsPagePrivateKeyPair.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public AccountTransactionsPagePrivateKeyPair(final Deserializer deserializer) {
		this.page = new AccountTransactionsPage(deserializer);
		this.privateKey = new PrivateKey(deserializer);
		final KeyPair keyPair = new KeyPair(this.privateKey);
		if (!Address.fromPublicKey(keyPair.getPublicKey()).equals(this.page.getAddress())) {
			throw new RuntimeException("private key in AccountTransactionsPagePrivateKeyPair must match supplied address");
		}
	}

	/**
	 * Gets the account transaction page.
	 *
	 * @return The page.
	 */
	public AccountTransactionsPage getPage() {
		return this.page;
	}

	/**
	 * Creates an AccountTransactionsPageBuilder from the page.
	 *
	 * @return The AccountTransactionsPageBuilder.
	 */
	public AccountTransactionsPageBuilder createPageBuilder() {
		final AccountTransactionsPageBuilder pageBuilder = new AccountTransactionsPageBuilder();
		pageBuilder.setAddress(page.getAddress().getEncoded());
		pageBuilder.setHash(null == page.getHash()? null : page.getHash().toString());
		pageBuilder.setId(null == page.getId()? null : page.getId().toString());
		return pageBuilder;
	}

	/**
	 * Gets the private key.
	 *
	 * @return The private key.
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Address.writeTo(serializer, "address", this.page.getAddress());
		if (null != this.page.getHash()) {
			serializer.writeString("hash", this.page.getHash().toString());
		}
		if (null != this.page.getId()) {
			serializer.writeString("id", this.page.getId().toString());
		}
	}
}
