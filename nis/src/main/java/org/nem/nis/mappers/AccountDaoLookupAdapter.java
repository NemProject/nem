package org.nem.nis.mappers;

import org.nem.core.model.Address;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.DbAccount;

import java.util.*;

/**
 * Adapts an AccountDao to an AccountDaoLookup.
 */
public class AccountDaoLookupAdapter implements AccountDaoLookup {

	private final AccountDao accountDao;
	private final Map<String, DbAccount> accountCache;

	/**
	 * Creates a new adapter that wraps an AccountDao.
	 *
	 * @param accountDao The AccountDao to wrap.
	 */
	public AccountDaoLookupAdapter(final AccountDao accountDao) {
		this.accountDao = accountDao;
		this.accountCache = new HashMap<>();
	}

	@Override
	public DbAccount findByAddress(final Address id) {
		final String encodedAddress = id.getEncoded();
		DbAccount dbAccount = this.accountCache.get(encodedAddress);
		final boolean isInCache = null != dbAccount;

		if (!isInCache) {
			dbAccount = this.accountDao.getAccountByPrintableAddress(encodedAddress);
		}

		if (null == dbAccount) {
			dbAccount = new DbAccount(encodedAddress, null);
		}

		if (null == dbAccount.getPublicKey()) {
			dbAccount.setPublicKey(id.getPublicKey());
		}

		if (!isInCache) {
			this.accountCache.put(encodedAddress, dbAccount);
		}

		return dbAccount;
	}
}
