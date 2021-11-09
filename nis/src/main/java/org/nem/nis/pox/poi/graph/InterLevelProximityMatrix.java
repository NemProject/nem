package org.nem.nis.pox.poi.graph;

import org.nem.core.math.*;
import org.nem.core.model.primitive.*;

import java.util.*;

/**
 * Container for the R and A sparse matrices that make up the inter-level proximity matrix. From the NCDawareRank paper by Nikolakopoulos
 * (WSDM 2013). <br>
 * The R and A matricies here are actually R(T) and A(T) in the paper. They are transposed because we use left stochastic matrices but the
 * paper uses right stochastic matrices.
 */
public class InterLevelProximityMatrix {
	/**
	 * Matrix that has incorporated information about: (1) the size of the cluster containing a node, (2) the number of clusters needed to
	 * cover the entire neighborhood of a node.
	 */
	private final Matrix r;

	/**
	 * Matrix that represents a binary mask denoting nodes in each cluster
	 */
	private final Matrix a;

	/**
	 * Creates an inter-level proximity matrix.
	 *
	 * @param clusters The results from the clustering process.
	 * @param neighborhood The neighborhoods of the nodes.
	 * @param outlinkMatrix The final outlink matrix (negative values removed), which represents the directed graph.
	 */
	public InterLevelProximityMatrix(final ClusteringResult clusters, final Neighborhood neighborhood, final Matrix outlinkMatrix) {
		final MatrixBuilder builder = new MatrixBuilder(clusters, neighborhood, outlinkMatrix);
		builder.build();
		this.r = builder.r;
		this.a = builder.a;
	}

	/**
	 * Returns the matrix r.
	 *
	 * @return The matrix r.
	 */
	public Matrix getR() {
		return this.r;
	}

	/**
	 * Returns the matrix a.
	 *
	 * @return the matrix a.
	 */
	public Matrix getA() {
		return this.a;
	}

	private class MatrixBuilder {
		private final ClusteringResult clusteringResult;
		private final Neighborhood neighborhood;
		private final Matrix outlinkMatrix;
		private final Matrix a;
		private final Matrix r;
		private final HashMap<ClusterId, SparseBitmap> clusterIdToNeighborhoodClusterIds = new HashMap<>();
		private int blockNum; // block == cluster; where cluster is { cluster, hub, outlier }

		public MatrixBuilder(final ClusteringResult clusteringResult, final Neighborhood neighborhood, final Matrix outlinkMatrix) {
			this.clusteringResult = clusteringResult;
			this.neighborhood = neighborhood;
			this.outlinkMatrix = outlinkMatrix;

			final int numNodes = this.clusteringResult.numNodes();
			final int numClusters = this.clusteringResult.numClusters();

			final int initialCapacity = Math.max(5, numNodes / numClusters);
			this.r = new SparseMatrix(numClusters, numNodes, initialCapacity);
			this.a = new SparseMatrix(numNodes, numClusters, initialCapacity);
		}

		/**
		 * Computes the two matrices needed for the NCD aware page rank calculation.
		 */
		private void build() {
			// 0) For every node compute the set of cluster ids which cover the entire neighborhood of the node
			final List<SparseBitmap> neighborhoodClusterIdsSet = this.computeNeighborhoodClusterIdsSet();

			// 1) Regular clusters
			this.processClusters(this.clusteringResult.getClusters(), neighborhoodClusterIdsSet);

			// 2) Hubs
			this.processClusters(this.clusteringResult.getHubs(), neighborhoodClusterIdsSet);

			// 3) Outliers
			this.processClusters(this.clusteringResult.getOutliers(), neighborhoodClusterIdsSet);
		}

		/**
		 * Computes the entries in r and a for the given clusters.
		 *
		 * @param clusters Collection of clusters.
		 * @param neighborhoodClusterIdsSet Set of clusters so that node u and all neighbors are contained in a cluster from xu.
		 */
		private void processClusters(final Collection<Cluster> clusters, final List<SparseBitmap> neighborhoodClusterIdsSet) {
			for (final Cluster cluster : clusters) {
				for (final NodeId id : cluster.getMemberIds()) {
					this.a.setAt(id.getRaw(), this.blockNum, 1);
				}

				final int clusterSize = cluster.size();
				final int currentRow = this.blockNum;
				final SparseBitmap ids = this.clusterIdToNeighborhoodClusterIds.getOrDefault(cluster.getId(), null);
				if (null != ids) {
					ids.toList().stream().forEach(
							i -> this.r.setAt(currentRow, i, 1.0 / (clusterSize * neighborhoodClusterIdsSet.get(i).cardinality())));
				}

				++this.blockNum;
			}
		}

		private List<SparseBitmap> computeNeighborhoodClusterIdsSet() {
			final List<SparseBitmap> neighborhoodClusterIdsSet = new ArrayList<>(this.outlinkMatrix.getColumnCount());
			for (int i = 0; i < this.outlinkMatrix.getColumnCount(); ++i) {
				final int id = i;
				final Collection<Community> neighboringCommunities = this.neighborhood.getNeighboringCommunities(new NodeId(id));
				final int[] clusterIdsForNode = neighboringCommunities.stream().map(community -> community.getPivotId().getRaw())
						.filter(rawPivotId -> rawPivotId == id || this.outlinkMatrix.getAt(rawPivotId, id) > 0.0).map(rawPivotId -> {
							// due to the way this function loops (with id incrementing and only setting id),
							// a SparseBitmap is safe because bits will be set in order
							ClusterId clusterId = this.clusteringResult.getIdForNode(new NodeId(rawPivotId));
							SparseBitmap ids = this.clusterIdToNeighborhoodClusterIds.get(clusterId);
							if (null == ids) {
								ids = SparseBitmap.createEmpty();
								this.clusterIdToNeighborhoodClusterIds.put(clusterId, ids);
							}

							ids.set(id);
							return clusterId;
						}).mapToInt(ClusterId::getRaw).toArray();

				// the cluster ids might not be in order, so we need to sort them first
				neighborhoodClusterIdsSet.add(SparseBitmap.createFromUnsortedData(clusterIdsForNode));
			}

			return neighborhoodClusterIdsSet;
		}
	}
}
