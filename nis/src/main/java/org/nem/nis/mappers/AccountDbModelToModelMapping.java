package org.nem.nis.mappers;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dbmodel.DbAccount;

/**
 * A mapping that is able to map a db account to a model account.
 */
public class AccountDbModelToModelMapping implements IMapping<DbAccount, Account> {
	private final AccountLookup accountLookup;

	/**
	 * Creates a new mapping.
	 *
	 * @param accountLookup The account lookup.
	 */
	public AccountDbModelToModelMapping(final AccountLookup accountLookup) {
		this.accountLookup = accountLookup;
	}

	@Override
	public Account map(final DbAccount dbAccount) {
		final Address address = null == dbAccount.getPublicKey()
				? Address.fromEncoded(dbAccount.getPrintableKey())
				: new AddressWithoutEncodedAddressGeneration(dbAccount.getPublicKey(), dbAccount.getPrintableKey());
		return this.accountLookup.findByAddress(address);
	}

	private static class AddressWithoutEncodedAddressGeneration extends Address {
		public AddressWithoutEncodedAddressGeneration(final PublicKey publicKey, final String encoded) {
			super(publicKey, encoded);
		}
	}
}