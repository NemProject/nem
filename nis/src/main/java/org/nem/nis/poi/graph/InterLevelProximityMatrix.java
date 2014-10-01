package org.nem.nis.poi.graph;

import org.nem.core.math.*;
import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO-CR [08062014][J-J]: i will need to look at this in more detail too

/**
 * Container for the R and A sparse matrices that make up the inter-level proximity matrix.
 * From the NCDawareRank paper by Nikolakopoulos (WSDM 2013).
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
	 * @param outlinkMatrix The final outlink matrix (negative values removed) which represents the directed graph.
	 */
	public InterLevelProximityMatrix(final ClusteringResult clusters, final Neighborhood neighborhood, final Matrix outlinkMatrix) {
		final int numNodes = clusters.numNodes();
		final int numClusters = clusters.numClusters();

		r = new SparseMatrix(numClusters, numNodes, 5);  //TODO: need to estimate good initial capacity.
		a = new SparseMatrix(numNodes, numClusters, 5); // TODO: need to estimate good initial capacity.
		clusterIdToNeighborhoodClusterIdsSetIndex = new HashMap<>();

		long t0 = System.currentTimeMillis();
		computeMatrices(clusters, neighborhood, outlinkMatrix);
		long t1 = System.currentTimeMillis();
		System.out.println("computing matrices took " + (t1 - t0) + "ms");
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
				a.setAt(id.getRaw(), row, 1);
			}

			final int clusterSize = cluster.size();
			final int currentRow = row;
			Set<Integer> indexSet = clusterIdToNeighborhoodClusterIdsSetIndex.getOrDefault(cluster.getId(),
					Collections.newSetFromMap(new ConcurrentHashMap<>()));
			indexSet.stream()
					.forEach(i -> r.setAt(currentRow, i, 1.0 / (clusterSize * neighborhoodClusterIdsSet.get(i).cardinality())));

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
			final SparseBitmap clusterIdsForNode = SparseBitmap.createFromUnsortedData(neighborhood.getNeighboringCommunities(new NodeId(id)).stream()
					.filter(c -> c.getPivotId().getRaw() == id || outlinkMatrix.getAt(c.getPivotId().getRaw(), id) > 0.0)
					.map(c -> {
						ClusterId clusterId = clusteringResult.getIdForNode(c.getPivotId());
						Set<Integer> indexSet = clusterIdToNeighborhoodClusterIdsSetIndex.get(clusterId);
						if (indexSet == null) {
							indexSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
							clusterIdToNeighborhoodClusterIdsSetIndex.put(clusterId, indexSet);
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
		return r;
	}

	/**
	 * Returns the matrix a.
	 *
	 * @return the matrix a.
	 */
	public SparseMatrix getA() {
		return a;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(this.r.toString() + System.lineSeparator());
		builder.append(this.a.toString());
		return builder.toString();
	}
}