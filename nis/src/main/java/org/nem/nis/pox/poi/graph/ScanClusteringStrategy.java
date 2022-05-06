package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;

/**
 * Implementation of the initial SCAN algorithm, from this paper: Xu, X., Yuruk, N., Feng, Z., &amp; Schweiger, T. A. (2007, August),
 * "ScanClusteringStrategy: a structural clustering algorithm for networks." <br>
 * In Proceedings of the 13th ACM SIGKDD international conference on Knowledge discovery and data mining (pp. 824-833). ACM.
 * http://www.ualr.edu/xwxu/publications/kdd07.pdf <br>
 * Given a set of vertices V and edges between the vertices E the paper defines when a vertex v is connected to a vertex w. This definition
 * gives rise to the definition of a relation in the set of vertices: <br>
 * For v,w &isin; V define v &sim; w &hArr; CONNECTε,μ(v,w) <br>
 * The relation &sim; is reflexive, symmetric and transitive, i.e. an equivalence relation. Thus the factorization V/&sim; is well defined
 * and induces a partition of V. The elements of the partition are called clusters. Aside from regular cluster the paper introduces the
 * notions of hubs and outliers.
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
			if (null != cluster) {
				this.addCluster(cluster);
			}
		}

		private Cluster buildCluster(final NodeId pivotId) {
			final List<ClusterId> overlappingClusters = new ArrayList<>();
			final Cluster cluster = new Cluster(pivotId);

			final ArrayDeque<NodeId> connections = new ArrayDeque<>();
			final Community pivotCommunity = this.getNeighborhood().getCommunity(pivotId);
			connections.addAll(pivotCommunity.getSimilarNeighbors().toList());
			final HashSet<NodeId> processedIds = new HashSet<>();
			while (!connections.isEmpty()) {
				final NodeId connectedNodeId = connections.pop();
				if (processedIds.contains(connectedNodeId)) {
					continue;
				}

				// dirReach = {x ∈ V | DirREACHε,μ(y, x)};
				// Note that DirREACH requires y to be a core node.
				// Here y = node with id connectedNodeId.
				final Community community = this.getNeighborhood().getCommunity(connectedNodeId);
				if (!community.isCore()) {
					continue;
				}

				final NodeNeighbors dirReach = community.getSimilarNeighbors();
				for (final NodeId nodeId : dirReach) {
					if (this.isClustered(nodeId)) {
						final ClusterId clusterId = new ClusterId(this.getNodeState(nodeId));
						if (!clusterId.equals(cluster.getId()) && !overlappingClusters.contains(clusterId)) {
							overlappingClusters.add(new ClusterId(this.getNodeState(nodeId)));
						}

						continue;
					}

					if (null == this.getNodeState(nodeId)) {
						// Need to analyze this node
						connections.add(nodeId);
					}

					// Assign to current cluster and add to community
					this.setNodeState(nodeId, cluster.getId());
					cluster.add(nodeId);
				}

				processedIds.add(connectedNodeId);
			}

			if (overlappingClusters.isEmpty()) {
				return cluster;
			}

			// need to merge clusters that overlap
			this.addCluster(cluster);
			overlappingClusters.add(cluster.getId());
			this.mergeClusters(overlappingClusters);
			return null;
		}
	}
}
