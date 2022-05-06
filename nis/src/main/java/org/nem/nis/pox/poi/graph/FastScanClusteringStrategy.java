package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Shiokawa et al., 2014, "構造的類似度に基づくグラフクラスタリングの高速化 (Fast Structural Similarity Graph Clustering),"
 * http://db-event.jpn.org/deim2014/final/proceedings/D6-2.pdf
 */
public class FastScanClusteringStrategy implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final FastScan impl = new FastScan(neighborhood);
		impl.scan();
		return impl.getClusters();
	}

	/**
	 * FastScan class uses the fast scan algorithm to cluster vertices (nodes) of a (transaction) graph into different groups: regular
	 * clusters, hubs and outliers.
	 */
	private static class FastScan extends AbstractScan {

		public FastScan(final Neighborhood neighborhood) {
			super(neighborhood);
		}

		@Override
		public void cluster(final Community community) {
			NodeNeighbors visited = new NodeNeighbors();
			visited.addNeighbor(community.getPivotId());
			this.processCommunity(community);

			NodeNeighbors unvisitedTwoHopPivots = this.getNeighborhood().getTwoHopAwayNeighbors(community.getPivotId());
			while (unvisitedTwoHopPivots.size() > 0) {
				this.clusterNeighbors(unvisitedTwoHopPivots);
				visited = NodeNeighbors.union(visited, unvisitedTwoHopPivots);
				unvisitedTwoHopPivots = this.getUnvisitedTwoHopUnion(unvisitedTwoHopPivots, visited);
			}
		}

		private void clusterNeighbors(final NodeNeighbors neighbors) {
			for (final NodeId v : neighbors) {
				final Community community = this.getNeighborhood().getCommunity(v);
				this.processCommunity(community);
			}
		}

		/**
		 * Calls processCommunityWithoutSpecialVisit and handles the special case that the node is at the end of a line graph.
		 *
		 * @param community The community to add.
		 */
		public void processCommunity(final Community community) {
			this.processCommunityWithoutSpecialVisit(community);

			if (2 != community.getSimilarNeighbors().size()) {
				return;
			}

			// Handle the special case that the node is at the end of a line graph. This doesn't seem
			// to work with regular FastScanClusteringStrategy because of the two-hop skipping
			// (pretty sure it is a limitation of the published algorithm).
			// We handle this case by creating a community around the neighboring node, rather
			// than just the two-hop away node. This is done only for that one neighbor and
			// does not propagate through subsequent iterations (i.e., we don't get the two-hop
			// away nodes for this one neighbor).
			for (final NodeId neighborId : community.getSimilarNeighbors()) {
				if (community.getPivotId().equals(neighborId)) {
					continue;
				}

				final Community neighborCommunity = this.getNeighborhood().getCommunity(neighborId);
				this.processCommunityWithoutSpecialVisit(neighborCommunity);
			}
		}

		/**
		 * If the community belongs to the core, then if the community overlaps an existing cluster, <br>
		 * - then merge the community into that cluster, <br>
		 * - else create a new cluster, <br>
		 * else mark community as non member. <br>
		 * <em>This function should only be called by processCommunity.</em>
		 *
		 * @param community The community to add.
		 */
		private void processCommunityWithoutSpecialVisit(final Community community) {
			if (!community.isCore()) {
				this.markAsNonCore(community);
				return;
			}

			final Cluster cluster;
			ClusterId clusterId = new ClusterId(community.getPivotId());

			// Find out if some of the similar neighbors already have an id.
			// This would mean the new cluster overlaps with at least one existing cluster.
			// In that case we merge the existing clusters and then expand the cluster.
			final List<ClusterId> clusterIds = community.getSimilarNeighbors().toList().stream().filter(this::isClustered)
					.map(this::getNodeState).distinct().map(ClusterId::new).collect(Collectors.toList());
			if (!clusterIds.isEmpty()) {
				cluster = this.mergeClusters(clusterIds);
				clusterId = cluster.getId();
			} else {
				cluster = new Cluster(clusterId);
				this.addCluster(cluster);
			}

			for (final NodeId nodeId : community.getSimilarNeighbors()) {
				this.setNodeState(nodeId, clusterId);
				cluster.add(nodeId);
			}
		}

		/**
		 * Returns a collection of all unvisited node ids that are two hops away from an already visited collection of nodes.
		 *
		 * @param newlyVisited Nodes visited in the last iteration of the while loop.
		 * @param allVisited All nodes visited up until now.
		 * @return The collection of nodes that are two hops away from the set of newlyVisited nodes.
		 */
		private NodeNeighbors getUnvisitedTwoHopUnion(final NodeNeighbors newlyVisited, final NodeNeighbors allVisited) {
			final NodeNeighbors[] twoHopAwayNodeNeighbors = new NodeNeighbors[newlyVisited.size()];

			int index = 0;
			for (final NodeId u : newlyVisited) {
				twoHopAwayNodeNeighbors[index++] = this.getNeighborhood().getTwoHopAwayNeighbors(u);
			}

			final NodeNeighbors newlyVisitedTwoHopAwayNodeNeighbors = NodeNeighbors.union(twoHopAwayNodeNeighbors);
			return newlyVisitedTwoHopAwayNodeNeighbors.difference(allVisited);
		}
	}
}
