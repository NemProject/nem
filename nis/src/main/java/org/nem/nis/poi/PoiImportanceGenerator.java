package org.nem.nis.poi;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;

import java.util.Collection;

/**
 * Interface for calculating the importance of a collection of accounts at a specific block height.
 */
public interface PoiImportanceGenerator {

	/**
	 * Updates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accounts The accounts.
	 */
	public default void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<Account> accounts) {
		this.updateAccountImportances(blockHeight, accounts, PoiScorer.ScoringAlg.MAKOTO);
	}

	/**
	 * Updates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accounts The accounts.
	 * @param scoringAlg The scoring algorithm.
	 */
	public void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<Account> accounts,
			final PoiScorer.ScoringAlg scoringAlg);
}
