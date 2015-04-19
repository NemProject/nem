package org.nem.core.model.ncc;

import org.nem.core.serialization.*;

/**
 * A pair containing both an account and account meta data.
 */
public class AccountMetaDataPair implements SerializableEntity {
	private final AccountInfo account;
	private final AccountMetaData metaData;

	/**
	 * Creates a new pair.
	 *
	 * @param account The account.
	 * @param metaData The meta data.
	 */
	public AccountMetaDataPair(final AccountInfo account, final AccountMetaData metaData) {
		this.account = account;
		this.metaData = metaData;
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public AccountMetaDataPair(final Deserializer deserializer) {
		this.account = deserializer.readObject("account", AccountInfo::new);
		this.metaData = deserializer.readObject("meta", AccountMetaData::new);
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
	public AccountInfo getAccount() {
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
