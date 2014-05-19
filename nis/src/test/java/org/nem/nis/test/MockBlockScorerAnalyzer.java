package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.model.Account;
import org.nem.core.model.BlockHeight;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.poi.PoiImportanceGenerator;

public class MockBlockScorerAnalyzer extends AccountAnalyzer {

	public MockBlockScorerAnalyzer() {
		super(Mockito.mock(PoiImportanceGenerator.class));
	}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight) {
		for (final Account account : this) {
			account.setImportance(blockHeight, account.getBalance(blockHeight, blockHeight).getNumMicroNem() / 1000.0);
		}
	}
}
