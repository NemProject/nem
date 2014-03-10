package org.nem.core.test;

import org.nem.core.model.Account;
import org.nem.core.model.ByteArray;
import org.nem.nis.AccountAnalyzer;

public class MockAccountAnalyzer extends AccountAnalyzer {
	public Account findByNemAddress(Account account) {
		return findByAddressImpl(null, account.getAddress().getEncoded());
	}

	public Account findByPublicKey(Account account) {
		return findByAddressImpl(new ByteArray(account.getKeyPair().getPublicKey()), null);
	}
}
