package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbAccount;
import org.nem.nis.mappers.IMapping;

/**
 * A mapping that is able to map an account id to a db account.
 */
public class AccountRawToDbModelMapping implements IMapping<Long, DbAccount> {

	@Override
	public DbAccount map(final Long id) {
		return null == id ? null : new DbAccount(id);
	}
}
