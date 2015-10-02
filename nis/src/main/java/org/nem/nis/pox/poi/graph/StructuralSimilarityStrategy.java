package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * A default similarity strategy based on structural similarity.
 */
public class StructuralSimilarityStrategy implements SimilarityStrategy {
	private final NodeNeighborMap neighborMap;

	/**
	 * Creates a new strategy.
	 *
	 * @param neighborMap The neighbor map.
	 */
	public StructuralSimilarityStrategy(final NodeNeighborMap neighborMap) {
		this.neighborMap = neighborMap;
	}

	@Override
	public double calculateSimilarity(final NodeId lhs, final NodeId rhs) {
		// The number of neighbors that are the same for the two input nodes, divided by
		// the sqrt of the product of the numbers of neighbors for each node.
		// Pivots are included in the calculation.

		final NodeNeighbors lhsNeighbors = this.neighborMap.getNeighbors(lhs);
		final NodeNeighbors rhsNeighbors = this.neighborMap.getNeighbors(rhs);

		final int lhsNeighborsSize = lhsNeighbors.size();
		final int rhsNeighborsSize = rhsNeighbors.size();
		final int commonNeighborSize = lhsNeighbors.commonNeighborsSize(rhsNeighbors);
		return commonNeighborSize / Math.sqrt(lhsNeighborsSize * rhsNeighborsSize);
	}
}
