package org.nem.core.mappers;

import org.nem.core.dbmodel.Account;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/* this is temporary solution
 * hopefully we'll do it differently
 */
public class DbAccountCache {
	private static ConcurrentMap<String, Account> accountCache = new ConcurrentHashMap<>();

	public static Account getAccountByPrintableAddress(String key) {
		return accountCache.get(key);
	}

	public static void addAccount(Account dbAccount) {
		accountCache.put(dbAccount.getPrintableKey(), dbAccount);
	}
}
