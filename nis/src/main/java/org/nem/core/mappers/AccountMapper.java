package org.nem.core.mappers;

import org.nem.core.dao.AccountDao;
import org.nem.core.model.Account;

public class AccountMapper {
	public static org.nem.core.dbmodel.Account toDbModel(final Account account, final AccountDao accountDao) {
		org.nem.core.dbmodel.Account dbAccount;

		final String encodedAddress = account.getAddress().getEncoded();
		final byte[] publicKey = null != account.getKeyPair() ? account.getKeyPair().getPublicKey() : null;

		dbAccount = DbAccountCache.getAccountByPrintableAddress(encodedAddress);
		if (dbAccount == null) {
			dbAccount = accountDao.getAccountByPrintableAddress(encodedAddress);
			if (dbAccount != null) {
				DbAccountCache.addAccount(dbAccount);
			}
		}
		if (null != dbAccount) {
			if (publicKey != null && dbAccount.getPublicKey() == null) {
				dbAccount.setPublicKey(publicKey);
			}
			return dbAccount;
		}

		dbAccount = new org.nem.core.dbmodel.Account(encodedAddress, publicKey);
		DbAccountCache.addAccount(dbAccount);
		return dbAccount;
	}
}
