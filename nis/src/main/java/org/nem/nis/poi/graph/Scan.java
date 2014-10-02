package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;

// TODO 20141001 J-M i think it makes more sense to name these classes (FastSca|Scan)ClusteringStrategy
// TODO 20141002 M-J Good idea

/**
 * Implementation of the initial SCAN algorithm, from this paper:
 * Xu, X., Yuruk, N., Feng, Z., & Schweiger, T. A. (2007, August),
 * "Scan: a structural clustering algorithm for networks."
 * <br/>
 * In Proceedings of the 13th ACM SIGKDD international conference
 * on Knowledge discovery and data mining (pp. 824-833). ACM.
 * http://www.ualr.edu/xwxu/publications/kdd07.pdf
 * <br/>
 * Given a set of vertices V and edges between the vertices E the paper defines when a vertex v is connected to a vertex w.
 * This definition gives rise to the definition of a relation in the set of vertices:
 * <br/>
 * For v,w ∈ V define v ~ w <==> CONNECTε,μ(v,w)
 * <br/>
 * The relation ~ is reflexive, symmetric and transitive, i.e. an equivalence relation.
 * Thus the factorization V/~ is well defined and induces a partition of V.
 * The elements of the partition are called clusters.
 * Aside from regular cluster the paper introduces the notions of hubs and outliers.
 */
public class Scan implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final ScanImpl impl = new ScanImpl(neighborhood);
		impl.scan();
		impl.processNonMembers();
		return impl.getClusters();
	}

	private static final int NON_MEMBER_CLUSTER_ID = -1;

	private static class ScanImpl {
		private final Neighborhood neighborhood;
		private final Integer neighborhoodSize;
		private final Integer[] nodeStates;
		private final List<Cluster> clusters = new ArrayList<>();
		private final List<Cluster> hubs = new ArrayList<>();
		private final List<Cluster> outliers = new ArrayList<>();

		public ScanImpl(final Neighborhood neighborhood) {
			this.neighborhood = neighborhood;
			this.neighborhoodSize = this.neighborhood.size();
			this.nodeStates = new Integer[this.neighborhoodSize];
		}

		public void scan() {
			for (int i = 0; i < this.neighborhoodSize; ++i) {
				if (null != this.nodeStates[i]) {
					continue;
				}

				final NodeId nodeId = new NodeId(i);
				final Community community = this.neighborhood.getCommunity(nodeId);
				if (!community.isCore()) {
					this.nodeStates[i] = NON_MEMBER_CLUSTER_ID;
					continue;
				}

				// build a community around i
				this.clusters.add(this.buildCluster(nodeId));
			}
		}

		public void processNonMembers() {
			for (int i = 0; i < this.neighborhoodSize; ++i) {
				if (NON_MEMBER_CLUSTER_ID != this.nodeStates[i]) {
					continue;
				}

				final Cluster cluster = new Cluster(new ClusterId(i));
				cluster.add(new NodeId(i));
				(this.isHub(this.neighborhood.getCommunity(new NodeId(i))) ? this.hubs : this.outliers).add(cluster);
			}
		}

		public boolean isHub(final Community community) {
			final HashSet<ClusterId> connectedClusterIds = new HashSet<>();
			return isHub(connectedClusterIds, community.getSimilarNeighbors()) ||
					isHub(connectedClusterIds, community.getDissimilarNeighbors());
		}

		private boolean isHub(final HashSet<ClusterId> connectedClusterIds, final NodeNeighbors neighbors) {
			// TODO 20141001 J-M since our graph is bidirectional, can there ever be a case where
			// > a hub can has two similar neighbors (i can't think of one)?
            // TODO 20141001 J-M: It depends on GraphConstants.MU.
			for (final NodeId neighborId : neighbors) {
				final int state = this.nodeStates[neighborId.getRaw()];
				if (NON_MEMBER_CLUSTER_ID == state) {
					continue;
				}

				connectedClusterIds.add(new ClusterId(state));
				if (connectedClusterIds.size() > 1) {
					return true;
				}
			}

			return false;
		}

		/**
		 * Gets the results of the clustering process.
		 *
		 * @return The clustering result.
		 */
		public ClusteringResult getClusters() {
			return new ClusteringResult(this.clusters, this.hubs, this.outliers);
		}

		private Cluster buildCluster(final NodeId pivotId) {
			final Cluster cluster = new Cluster(new ClusterId(pivotId));

			final ArrayDeque<NodeId> connections = new ArrayDeque<>();
			final Community pivotCommunity = this.neighborhood.getCommunity(pivotId);
			connections.addAll(pivotCommunity.getSimilarNeighbors().toList());
			while (!connections.isEmpty()) {
				final NodeId connectedNodeId = connections.pop();

				// dirReach = {x ∈ V | DirREACHε,μ(y, x)};
				// Note that DirREACH requires y to be a core node.
				// Here y = node with id connectedNodeId.
				final Community community = this.neighborhood.getCommunity(connectedNodeId);
				if (!community.isCore()) {
					continue;
				}

				final NodeNeighbors dirReach = community.getSimilarNeighbors();
				for (final NodeId nodeId : dirReach) {
					final int id = nodeId.getRaw();
					if (this.isClustered(id)) {
						continue;
					}

					if (null == this.nodeStates[id]) {
						// Need to analyze this node
						connections.add(nodeId);
					}

					// Assign to current cluster and add to community
					this.nodeStates[id] = pivotId.getRaw();
					cluster.add(nodeId);
				}
			}

			return cluster;
		}

		private boolean isClustered(final int id) {
			return null != this.nodeStates[id] && NON_MEMBER_CLUSTER_ID != this.nodeStates[id];
		}
	}
}
