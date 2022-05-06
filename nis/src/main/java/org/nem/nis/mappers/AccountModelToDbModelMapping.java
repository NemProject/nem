package org.nem.nis.mappers;

import org.nem.core.model.Account;
import org.nem.nis.dbmodel.DbAccount;

/**
 * A mapping that is able to map a model account to a db account.
 */
public class AccountModelToDbModelMapping implements IMapping<Account, DbAccount> {
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
	public DbAccount map(final Account account) {
		return this.accountDaoLookup.findByAddress(account.getAddress());
	}
}
