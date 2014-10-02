package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;

/**
 * Implementation of the initial SCAN algorithm, from this paper:
 * Xu, X., Yuruk, N., Feng, Z., & Schweiger, T. A. (2007, August),
 * "ScanClusteringStrategy: a structural clustering algorithm for networks."
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
public class ScanClusteringStrategy implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final Scan impl = new Scan(neighborhood);
		impl.scan();
		return impl.getClusters();
	}

	private static class Scan extends AbstractScan {

		public Scan(final Neighborhood neighborhood) {
			super(neighborhood);
		}

		@Override
		public void cluster(final Community community) {
			// build a cluster around the community
			final Cluster cluster = this.buildCluster(community.getPivotId());
			this.clusters.add(cluster);
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
	}
}
