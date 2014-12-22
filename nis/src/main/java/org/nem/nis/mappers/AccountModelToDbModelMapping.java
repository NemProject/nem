package org.nem.nis.mappers;

import org.nem.core.model.Account;

/**
 * A mapping that is able to map a model account to a db account.
 */
public class AccountModelToDbModelMapping implements IMapping<Account, org.nem.nis.dbmodel.Account> {
	private final AccountDaoLookup accountDaoLookup;

	/**
	 * Creates a new mapping.
	 *
	 * @param accountDaoLookup The account dao lookup.
	 */
	public AccountModelToDbModelMapping(final AccountDaoLookup accountDaoLookup) {
		this.accountDaoLookup = accountDaoLookup;
	}

	@Override
	public org.nem.nis.dbmodel.Account map(final Account account) {
		return this.accountDaoLookup.findByAddress(account.getAddress());
	}
}
