package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;
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
	 * FastScan class uses the fast scan algorithm to cluster vertices (nodes) of a (transaction) graph
	 * into different groups: regular clusters, hubs and outliers.
	 */
	private static class FastScan extends AbstractScan {

		public FastScan(final Neighborhood neighborhood) {
			super(neighborhood);
		}

		/**
		 * The clustering process:
		 * Group a neighborhood into clusters, hubs and outliers.
		 */
		@Override
		public void findClusters() {
			NodeNeighbors visited = new NodeNeighbors();
			for (int i = 0; i < this.neighborhoodSize; ++i) {
				if (null != this.nodeStates[i]) {
					continue;
				}

				final NodeId nodeId = new NodeId(i);
				visited.removeAll();
				visited.addNeighbor(nodeId);

				final Community community = this.neighborhood.getCommunity(nodeId);
				addToCluster(community, community.getPivotId());

				NodeNeighbors unvisitedTwoHopPivots = this.neighborhood.getTwoHopAwayNeighbors(nodeId);

				// Handle special case where we are the end of a line
				if (community.getSimilarNeighbors().size() == 2) {
					specialVisit(new NodeId(i), community);
				}

				while (0 < unvisitedTwoHopPivots.size()) {
					for (final NodeId v : unvisitedTwoHopPivots) {
						final Community c = this.neighborhood.getCommunity(v);
						this.addToCluster(c, c.getPivotId());

						// Handle special case where we are the end of a line
						if (c.getSimilarNeighbors().size() == 2) {
							specialVisit(v, c);
						}
					}
					visited = NodeNeighbors.union(visited, unvisitedTwoHopPivots);
					unvisitedTwoHopPivots = getVisitedTwoHopUnion(unvisitedTwoHopPivots, visited);
				}
			}
		}

		/**
		 * Handle the special case that the node is at the end of a line graph. This doesn't seem
		 * to work with regular FastScanClusteringStrategy because of the two-hop skipping (pretty sure it is a limitation
		 * of the published algorithm).
		 * We handle this case by creating a community around the neighboring node, rather
		 * than just the two-hop away node. This is done only for that one neighbor and
		 * does not propagate through subsequent iterations (i.e., we don't get the two-hop
		 * away nodes for this one neighbor).
		 *
		 * @param curNode - current node
		 * @param c - community for the current node
		 */
		public void specialVisit(final NodeId curNode, final Community c) {
			for (final NodeId simNeighbor : c.getSimilarNeighbors()) {
				if (curNode == simNeighbor) {
					continue;
				}
				final Community specialVisit = this.neighborhood.getCommunity(simNeighbor);
				this.addToCluster(specialVisit, specialVisit.getPivotId());
			}
		}

		/**
		 * If the community belongs to the core,
		 * then if the community overlaps an existing cluster,
		 * - then merge the community into that cluster,
		 * - else create a new cluster,
		 * else mark community as non member.
		 *
		 * @param community The community to add.
		 * @param id The id for a new cluster.
		 */
		private void addToCluster(final Community community, final NodeId id) {
			if (!community.isCore()) {
				if (null == this.nodeStates[community.getPivotId().getRaw()]) {
					this.nodeStates[community.getPivotId().getRaw()] = NON_MEMBER_CLUSTER_ID;
				}
			} else {
				final Cluster cluster;
				ClusterId tmp = new ClusterId(id);

				// Find out if some of the similar neighbors already have an id.
				// This would mean the new cluster overlaps with at least one existing cluster.
				// In that case we merge the existing clusters and then expand the cluster.
				final List<ClusterId> clusterIds = community.getSimilarNeighbors().toList().stream()
						.filter(nodeId -> null != this.nodeStates[nodeId.getRaw()] && NON_MEMBER_CLUSTER_ID != this.nodeStates[nodeId.getRaw()])
						.map(nodeId -> this.nodeStates[nodeId.getRaw()])
						.distinct()
						.map(ClusterId::new)
						.collect(Collectors.toList());
				if (!clusterIds.isEmpty()) {
					cluster = mergeClusters(clusterIds);
					tmp = cluster.getId();
				} else {
					cluster = new Cluster(tmp);
					this.clusters.add(cluster);
				}
				final ClusterId clusterId = tmp;
				for (final NodeId nodeId : community.getSimilarNeighbors()) {
					this.nodeStates[nodeId.getRaw()] = clusterId.getRaw();
					cluster.add(nodeId);
				}
			}
		}

		/**
		 * Merge existing clusters.
		 *
		 * @param clusterIds The ids of the clusters to merge
		 * @return The merged cluster.
		 */
		private Cluster mergeClusters(final List<ClusterId> clusterIds) {
			if (0 >= clusterIds.size()) {
				throw new IllegalArgumentException("need at least one cluster id to merge");
			}

			final ClusterId clusterId = clusterIds.get(0);
			final Cluster cluster = findCluster(clusterId);

			for (int ndx = 1; ndx < clusterIds.size(); ndx++) {
				final Cluster clusterToMerge = findCluster(clusterIds.get(ndx));
				cluster.merge(clusterToMerge);
				clusterToMerge.getMemberIds().stream().forEach(id -> this.nodeStates[id.getRaw()] = clusterId.getRaw());
				this.clusters.remove(clusterToMerge);
			}

			return cluster;
		}

		/**
		 * Returns a collection of all unvisited node ids that are two hops away
		 * from an already visited collection of nodes.
		 *
		 * @param newlyVisited - Nodes visited in the last iteration of the while loop.
		 * @param visitedTwoHopUnion - nodes visited up until now.
		 * @return The collection of nodes that are two hops away from the set of newlyVisited nodes.
		 */
		private NodeNeighbors getVisitedTwoHopUnion(final NodeNeighbors newlyVisited, final NodeNeighbors visitedTwoHopUnion) {
			final NodeNeighbors[] twoHopAwayNodeNeighbors = new NodeNeighbors[newlyVisited.size()];

			int index = 0;
			for (final NodeId u : newlyVisited) {
				twoHopAwayNodeNeighbors[index++] = this.neighborhood.getTwoHopAwayNeighbors(u);
			}

			NodeNeighbors unvisitedTwoHopUnion = NodeNeighbors.union(twoHopAwayNodeNeighbors);
			unvisitedTwoHopUnion = unvisitedTwoHopUnion.difference(visitedTwoHopUnion);
			return unvisitedTwoHopUnion;
		}

		/**
		 * Returns the cluster with a given id from the cluster collection.
		 *
		 * @param clusterId The id for the cluster.
		 * @return The cluster with the wanted id.
		 */
		private Cluster findCluster(final ClusterId clusterId) {
			for (final Cluster cluster : this.clusters) {
				if (cluster.getId().equals(clusterId)) {
					return cluster;
				}
			}

			throw new IllegalArgumentException("cluster with id " + clusterId + " not found.");
		}
	}
}
