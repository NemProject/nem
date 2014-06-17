package org.nem.core.model.ncc;

import org.nem.core.model.Account;
import org.nem.core.serialization.*;

public class AccountMetaDataPair implements SerializableEntity {

	private Account account;
	private AccountMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param account The account.
	 * @param metaData The meta data.
	 */
	public AccountMetaDataPair(final Account account, final AccountMetaData metaData) {
		this.account = account;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public AccountMetaDataPair(final Deserializer deserializer) {
		this(deserializer.readObject("account", obj -> new Account(obj)),
			 deserializer.readObject("meta", obj -> new AccountMetaData(obj)));
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("account", this.account);
		serializer.writeObject("meta", this.metaData);
	}

	/**
	 * Gets the account.
	 *
	 * @return The account.
	 */
	public Account getAccount() {
		return this.account;
	}

	/**
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public AccountMetaData getMetaData() {
		return this.metaData;
	}
}
