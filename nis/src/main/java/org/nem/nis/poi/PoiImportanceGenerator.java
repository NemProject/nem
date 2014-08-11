package org.nem.nis.poi;

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
	 * @param accountStates The account states.
	 */
	public default void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		this.updateAccountImportances(blockHeight, accountStates, PoiScorer.ScoringAlg.MAKOTO);
	}

	/**
	 * Updates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accountStates The account states.
	 * @param scoringAlg The scoring algorithm.
	 */
	public void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates,
			final PoiScorer.ScoringAlg scoringAlg);
}
