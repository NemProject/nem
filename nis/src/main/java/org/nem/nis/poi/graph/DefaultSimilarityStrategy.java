package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * A default similarity strategy.
 */
public class DefaultSimilarityStrategy implements SimilarityStrategy {
	private final NodeNeighborMap neighborMap;

	/**
	 * Creates a new strategy.
	 *
	 * @param neighborMap The neighbor map.
	 */
	public DefaultSimilarityStrategy(final NodeNeighborMap neighborMap) {
		this.neighborMap = neighborMap;
	}

	@Override
	public double calculateSimilarity(final NodeId lhs, final NodeId rhs) {
		final NodeNeighbors lhsNeighbors = this.neighborMap.getNeighbors(lhs);
		final NodeNeighbors rhsNeighbors = this.neighborMap.getNeighbors(rhs);

		final int lhsNeighborsSize = lhsNeighbors.size();
		final int rhsNeighborsSize = rhsNeighbors.size();
		final int commonNeighborSize = lhsNeighbors.commonNeighborsSize(rhsNeighbors);
		return commonNeighborSize / Math.sqrt(lhsNeighborsSize * rhsNeighborsSize);
	}
}
