package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbAccount;
import org.nem.nis.mappers.IMapping;

/**
 * A mapping that is able to map an account id to a db account.
 */
public class AccountRawToDbModelMapping implements IMapping<Long, DbAccount> {

	@Override
	public DbAccount map(final Long id) {
		if (null == id) {
			return null;
		}

		final DbAccount dbAccount = new DbAccount();
		dbAccount.setId(id);
		return dbAccount;
	}
}
