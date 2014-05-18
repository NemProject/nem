package org.nem.nis;

import java.util.List;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.BlockHeight;
import org.nem.nis.poi.PoiScorer;

/**
 * This is the interface for POI. We may want to try different specific
 * implementations, so I moved this out to make that easier.
 */
public interface Poi {
	public ColumnVector getAccountImportances(BlockHeight blockHeight, List<Account> accounts);
	public ColumnVector getAccountImportances(BlockHeight blockHeight, List<Account> accounts, PoiScorer.ScoringAlg scoringAlg);
}
