package org.nem.nis.test;

import org.nem.core.model.Account;
import org.nem.nis.AccountAnalyzer;

public class MockAccountAnalyzer extends AccountAnalyzer {
	public Account findByNemAddress(Account account) {
		return findByAddressImpl(null, account.getAddress().getEncoded());
	}

	public Account findByPublicKey(Account account) {
		return findByAddressImpl(account.getKeyPair().getPublicKey(), null);
	}
}
