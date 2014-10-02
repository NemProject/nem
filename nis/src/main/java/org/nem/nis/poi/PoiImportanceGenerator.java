package org.nem.nis.poi;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.graph.*;

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
		// TODO-CR [08062014][J-M]: based on the algo, it looks like the effect of clustering is additive
		// (the clustering score is added to the original importance at each step)
		// TODO-CR [20140806][M-J]: do you mean the NCDawareRank is additive w.r.t. PageRank? If so, it is just another term, so you could probably say that
		// in that case, can we inject the step strategy instead of the clustering algo?
		// then it should be easier to test the steps and we can have two one that's unaware of clustering
		// and a clustering-aware one that decorates it
		this.updateAccountImportances(blockHeight, accountStates, PoiScorer.ScoringAlg.MAKOTO, new FastScan());
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
			final PoiScorer.ScoringAlg scoringAlg,
			final GraphClusteringStrategy clusterer);
}
