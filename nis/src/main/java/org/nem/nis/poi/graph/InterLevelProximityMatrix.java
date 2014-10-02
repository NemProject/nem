package org.nem.nis.poi.graph;

import org.nem.core.math.*;
import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for the R and A sparse matrices that make up the inter-level proximity matrix.
 * From the NCDawareRank paper by Nikolakopoulos (WSDM 2013).
 * <br/>
 * The R and A matricies here are actually R(T) and A(T) in the paper.
 * They are transposed because we use right stochastic matrices but the paper uses left stochastic matrices.
 */
public class InterLevelProximityMatrix {
	/**
	 * Matrix that has incorporated information about:
	 * 1) the size of the cluster containing a node.
	 * 2) the number of clusters needed to cover the entire neighborhood of a node.
	 */
	private final SparseMatrix r;

	/**
	 * Matrix that represents a binary mask denoting nodes in each cluster
	 */
	private final SparseMatrix a;

	private final HashMap<ClusterId, Set<Integer>> clusterIdToNeighborhoodClusterIdsSetIndex;

	/**
	 * Creates an inter-level proximity matrix.
	 *
	 * @param clusters The results from the clustering process.
	 * @param neighborhood The neighborhoods of the nodes.
	 * @param outlinkMatrix The final outlink matrix (negative values removed), which represents the directed graph.
	 */
	public InterLevelProximityMatrix(final ClusteringResult clusters, final Neighborhood neighborhood, final Matrix outlinkMatrix) {
		final int numNodes = clusters.numNodes();
		final int numClusters = clusters.numClusters();

		this.r = new SparseMatrix(numClusters, numNodes, 5);  //TODO: need to estimate good initial capacity.
		this.a = new SparseMatrix(numNodes, numClusters, 5); // TODO: need to estimate good initial capacity.
		this.clusterIdToNeighborhoodClusterIdsSetIndex = new HashMap<>();

		computeMatrices(clusters, neighborhood, outlinkMatrix);
	}

	/**
	 * Computes the two matrices needed for the NCD aware page rank calculation.
	 *
	 * @param clusteringResult The results from the clustering process.
	 * @param neighborhood The neighborhoods of the nodes.
	 * @param outlinkMatrix The final outlink matrix (negative values removed) which represents the directed graph.
	 */
	private void computeMatrices(final ClusteringResult clusteringResult, final Neighborhood neighborhood, final Matrix outlinkMatrix) {
		int blockNum = 0;

		// 0) For every node compute the set of cluster ids which cover the entire neighborhood of the node
		final List<SparseBitmap> neighborhoodClusterIdsSet = computeNeighborhoodClusterIdsSet(clusteringResult, neighborhood, outlinkMatrix);

		// 1) Regular clusters
		handleClusters(clusteringResult.getClusters(), neighborhoodClusterIdsSet, blockNum);
		blockNum += clusteringResult.getClusters().size();

		// 2) Hubs
		handleClusters(clusteringResult.getHubs(), neighborhoodClusterIdsSet, blockNum);
		blockNum += clusteringResult.getHubs().size();

		// 3) Outliers
		handleClusters(clusteringResult.getOutliers(), neighborhoodClusterIdsSet, blockNum);
	}

	/**
	 * Computes the entries in r and a for the given clusters.
	 *
	 * @param clusters Collection of clusters.
	 * @param neighborhoodClusterIdsSet Set of clusters so that node u and all neighbors are contained in a cluster from xu.
	 * @param row The row into which to put the values.
	 */
	private void handleClusters(final Collection<Cluster> clusters, final List<SparseBitmap> neighborhoodClusterIdsSet, int row) {
		for (final Cluster cluster : clusters) {

			for (final NodeId id : cluster.getMemberIds()) {
				this.a.setAt(id.getRaw(), row, 1);
			}

			final int clusterSize = cluster.size();
			final int currentRow = row;
			final Set<Integer> indexSet = this.clusterIdToNeighborhoodClusterIdsSetIndex.getOrDefault(cluster.getId(),
					Collections.newSetFromMap(new ConcurrentHashMap<>()));
			indexSet.stream()
					.forEach(i -> this.r.setAt(currentRow, i, 1.0 / (clusterSize * neighborhoodClusterIdsSet.get(i).cardinality())));

			++row;
		}
	}

	private List<SparseBitmap> computeNeighborhoodClusterIdsSet(
			final ClusteringResult clusteringResult,
			final Neighborhood neighborhood,
			final Matrix outlinkMatrix) {
		final List<SparseBitmap> neighborhoodClusterIdsSet = new ArrayList<>(outlinkMatrix.getColumnCount());

		for (int i = 0; i < outlinkMatrix.getColumnCount(); ++i) {
			final int id = i;
			// TODO: does it make sense for getNeighboringCommunities to return sorted ids?
			final SparseBitmap clusterIdsForNode = SparseBitmap.createFromUnsortedData(neighborhood.getNeighboringCommunities(new NodeId(id)).stream()
					.filter(c -> c.getPivotId().getRaw() == id || outlinkMatrix.getAt(c.getPivotId().getRaw(), id) > 0.0)
					.map(c -> {
						ClusterId clusterId = clusteringResult.getIdForNode(c.getPivotId());
						Set<Integer> indexSet = this.clusterIdToNeighborhoodClusterIdsSetIndex.get(clusterId);
						if (indexSet == null) {
							indexSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
							this.clusterIdToNeighborhoodClusterIdsSetIndex.put(clusterId, indexSet);
						}
						indexSet.add(id);
						return clusterId;
					})
					.mapToInt(ClusterId::getRaw).toArray());
			neighborhoodClusterIdsSet.add(clusterIdsForNode);
		}

		return neighborhoodClusterIdsSet;
	}

	/**
	 * Returns the matrix r.
	 *
	 * @return The matrix r.
	 */
	public SparseMatrix getR() {
		return this.r;
	}

	/**
	 * Returns the matrix a.
	 *
	 * @return the matrix a.
	 */
	public SparseMatrix getA() {
		return this.a;
	}
}