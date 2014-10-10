package org.nem.nis.poi;

import org.nem.core.model.primitive.Amount;
import org.nem.nis.poi.graph.GraphClusteringStrategy;

/**
 * Options that influence the POI calculation.
 */
public interface PoiOptions {

	//region weights

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
	 * Gets the weight given to (net) negative outlinks.
	 *
	 * @return The weight.
	 */
	public double getNegativeOutlinkWeight();

	/**
	 * Gets the weight given to outlier nodes.
	 *
	 * @return The weight.
	 */
	public double getOutlierWeight();

	//endregion

	//region clustering

	/**
	 * Gets a value indicating whether or not clustering is enabled.
	 *
	 * @return true if clustering should be enabled.
	 */
	public boolean isClusteringEnabled();

	/**
	 * Gets the clustering strategy.
	 *
	 * @return The graph clustering strategy.
	 */
	public GraphClusteringStrategy getClusteringStrategy();

	/**
	 * Gets the mu clustering variable.
	 * <br/>
	 * The minimum number of neighbors with high structural similarity that
	 * a node must have to be considered core.
	 * A node itself is considered as neighbor of itself (it is in its set of similar neighbors).
	 *
	 * @return The mu value.
	 */
	public int getMuClusteringValue();

	/**
	 * Gets the epsilon clustering variable.
	 * <br/>
	 * The structural similarity threshold that will cause nodes to be considered
	 * highly similar (if they have a similarity greater than this value).
	 *
	 * @return The epsilon value.
	 */
	public double getEpsilonClusteringValue();

	//endregion

	//region teleportation

	/**
	 * Gets the teleportation probability.
	 *
	 * @return The teleportation probability.
	 */
	public double getTeleportationProbability();

	/**
	 * Gets the inter-level teleportation probability.
	 *
	 * @return The inter-level teleportation probability.
	 */
	public double getInterLevelTeleportationProbability();

	/**
	 * Gets the inverse teleportation probability.
	 *
	 * @return The inverse teleportation probability.
	 */
	public default double getInverseTeleportationProbability() {
		return 1.0 - this.getTeleportationProbability() - this.getInterLevelTeleportationProbability();
	}

	//endregion
}
