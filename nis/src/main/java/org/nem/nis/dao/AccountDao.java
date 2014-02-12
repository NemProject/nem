package org.nem.nis.dao;

import java.util.List;

import org.nem.nis.model.Account;

public interface AccountDao {
	public Account getAccount(Long id);
	
	public Account getAccountByPrintableAddress(byte[] printableAddres);
	
	public void save(Account account);
	
	public Long count();

	public void saveMulti(List<Account> recipientsAccounts);
}
