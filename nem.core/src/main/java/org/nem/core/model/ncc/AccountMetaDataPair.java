package org.nem.core.model.ncc;

import org.nem.core.serialization.Deserializer;

/**
 * A pair containing both an account and account meta data.
 */
public class AccountMetaDataPair extends AbstractMetaDataPair<AccountInfo, AccountMetaData> {

	/**
	 * Creates a new pair.
	 *
	 * @param account The account.
	 * @param metaData The meta data.
	 */
	public AccountMetaDataPair(final AccountInfo account, final AccountMetaData metaData) {
		super("account", "meta", account, metaData);
	}

	/**
	 * Deserializes a pair.
	 *
	 * @param deserializer The deserializer
	 */
	public AccountMetaDataPair(final Deserializer deserializer) {
		super("account", "meta", AccountInfo::new, AccountMetaData::new, deserializer);
	}
}
