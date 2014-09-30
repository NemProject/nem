package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * Strategy for calculating the similarity between two nodes.
 */
@FunctionalInterface
public interface SimilarityStrategy {

	/**
	 * Calculates structural similarity between two nodes. This is the number of
	 * neighbors that are the same for the two input nodes, divided by
	 * the sqrt of the product of the numbers of neighbors for each node.
	 * Pivots are included in the calculation.
	 *
	 * @param lhs One node id.
	 * @param rhs The other node id.
	 * @return The similarity score.
	 */
	public double calculateSimilarity(final NodeId lhs, final NodeId rhs);
}
