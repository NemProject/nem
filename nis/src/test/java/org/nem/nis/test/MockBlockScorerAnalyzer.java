package org.nem.nis.test;

import org.nem.core.model.Account;
import org.nem.core.model.BlockHeight;
import org.nem.nis.AccountAnalyzer;

import java.util.Iterator;

public class MockBlockScorerAnalyzer extends AccountAnalyzer {

	@Override
	public void recalculateImportances(final BlockHeight blockHeight) {
		final Iterator<Account> accountIterator = iterator();
		while (accountIterator.hasNext()) {
			final Account account = accountIterator.next();
			account.setImportance(blockHeight, account.getBalance(blockHeight, blockHeight).getNumMicroNem()/1000.0);
		}
	}
}
