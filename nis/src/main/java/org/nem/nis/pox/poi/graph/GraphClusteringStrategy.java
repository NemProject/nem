package org.nem.nis.pox.poi.graph;

/**
 * Interface for graph clustering strategies.
 */
public interface GraphClusteringStrategy {

	/**
	 * Clusters a neighborhood of nodes in a graph.
	 *
	 * @param neighborhood The neighborhood.
	 * @return The result of the clustering operation.
	 */
	ClusteringResult cluster(final Neighborhood neighborhood);
}
