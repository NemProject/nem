package org.nem.nis.mappers;

import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.Account;
import org.nem.core.model.Address;

import java.util.*;

/**
 * Adapts an AccountDao to an AccountDaoLookup.
 */
public class AccountDaoLookupAdapter implements AccountDaoLookup {

	private final AccountDao accountDao;
	private final Map<String, Account> accountCache;

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
	public Account findByAddress(final Address id) {
		final String encodedAddress = id.getEncoded();
		Account dbAccount = this.accountCache.get(encodedAddress);
		boolean isInCache = null != dbAccount;

		if (!isInCache)
			dbAccount = this.accountDao.getAccountByPrintableAddress(encodedAddress);

		if (null == dbAccount)
			dbAccount = new Account(encodedAddress, null);

		if (null == dbAccount.getPublicKey())
			dbAccount.setPublicKey(id.getPublicKey());

		if (!isInCache)
			this.accountCache.put(encodedAddress, dbAccount);

		return dbAccount;
	}
}
