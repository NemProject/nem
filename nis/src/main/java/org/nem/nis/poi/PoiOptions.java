package org.nem.nis.poi;

import org.nem.core.model.primitive.Amount;

/**
 * Options that influence the POI calculation.
 */
public interface PoiOptions {

	/**
	 * Gets the minimum (vested) balance required for a harvester.
	 *
	 * @return The minimum (vested) balance.
	 */
	public Amount getMinHarvesterBalance();

	/**
	 * Gets the minimum outlink weight required for an outlink to be included in the POI calculation.
	 *
	 * @return The minimum outlink weight.
	 */
	public Amount getMinOutlinkWeight();

	/**
	 * Gets a value indicating whether or not clustering is enabled.
	 *
	 * @return true if clustering should be enabled.
	 */
	public boolean isClusteringEnabled();
}
