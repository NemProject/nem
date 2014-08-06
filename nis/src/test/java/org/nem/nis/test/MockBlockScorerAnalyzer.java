package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.poi.PoiImportanceGenerator;

public class MockBlockScorerAnalyzer extends AccountAnalyzer {

	public MockBlockScorerAnalyzer() {
		super(Mockito.mock(PoiImportanceGenerator.class));
	}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight) {
		for (final Account account : this) {
			final Amount balance = account.getWeightedBalances().getUnvested(blockHeight);
			final double importance = balance.getNumMicroNem() / 1000.0;
			account.getImportanceInfo().setImportance(blockHeight, importance);
		}
	}
}
