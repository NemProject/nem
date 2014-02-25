package org.nem.nis.virtual;

import java.util.LinkedList;
import java.util.List;

import org.nem.core.dbmodel.Account;

public class VirtualAccounts {
	public VirtualAccounts() {
		m_accounts = new LinkedList<Account>();
	}
	
	public void add(Account a) {
		m_accounts.add(a);
	}
	
	private List<Account> m_accounts;
}
