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
	default public void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		this.updateAccountImportances(blockHeight, accountStates, new PoiScorer(), new FastScanClusteringStrategy());
	}

	/**
	 * Updates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accountStates The account states.
	 * @param poiScorer The poi scorer.
	 * @param clusterer The clustering strategy.
	 */
	public void updateAccountImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates,
			final PoiScorer poiScorer,
			final GraphClusteringStrategy clusterer);
}
