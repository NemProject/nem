package org.nem.nis.poi;

import java.util.Collection;

import org.nem.core.math.ColumnVector;
import org.nem.core.model.Account;
import org.nem.core.model.BlockHeight;

/**
 * Interface for calculating the importance of a collection of accounts at a specific block height.
 */
public interface PoiImportanceGenerator {

	/**
	 * Generates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accounts The accounts.
	 * @return The importance scores.
	 */
	public default ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<Account> accounts) {
		return this.getAccountImportances(blockHeight, accounts, PoiScorer.ScoringAlg.MAKOTO);
	}

	/**
	 * Generates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accounts The accounts.
	 * @param scoringAlg The scoring algorithm.
	 * @return The importance scores.
	 */
	public ColumnVector getAccountImportances(
			final BlockHeight blockHeight,
			final Collection<Account> accounts,
			final PoiScorer.ScoringAlg scoringAlg);
}
