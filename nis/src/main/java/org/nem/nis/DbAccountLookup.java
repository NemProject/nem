package org.nem.nis;

import org.nem.core.crypto.KeyPair;
import org.nem.core.dao.AccountDao;
import org.nem.core.model.Account;
import org.nem.core.model.Address;
import org.nem.core.serialization.AccountLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/**
 * this is temporary class, we'll probably keep all accounts data in memory,
 */
@Component
public class DbAccountLookup implements AccountLookup {
	private static final Logger logger = Logger.getLogger(DbAccountLookup.class.getName());

	@Autowired
	private AccountDao accountDao;

	@Override
	public Account findByAddress(final Address id) {
		logger.info(id.getEncoded());
		logger.warning(accountDao.toString());
		logger.info(id.getEncoded());

		org.nem.core.dbmodel.Account dbAccount = accountDao.getAccountByPrintableAddress(id.getEncoded());
		if (dbAccount == null) {
			throw new RuntimeException("account not found in the db");
		}

		return new Account(new KeyPair(dbAccount.getPublicKey()));
	}
}
