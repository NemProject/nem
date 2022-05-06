package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;

/**
 * The result of a clustering operation.
 */
public class ClusteringResult {
	private final Collection<Cluster> clusters;
	private final Collection<Cluster> hubs;
	private final Collection<Cluster> outliers;
	private final HashMap<NodeId, ClusterId> idMap; // key := node id, value := node's cluster id

	/**
	 * Creates a new clustering result.
	 *
	 * @param clusters The regular clusters.
	 * @param hubs The hub clusters.
	 * @param outliers The outlier clusters.
	 */
	public ClusteringResult(final Collection<Cluster> clusters, final Collection<Cluster> hubs, final Collection<Cluster> outliers) {
		this.clusters = clusters;
		this.hubs = hubs;
		this.outliers = outliers;

		// build id map, needed to keep track of what clusters contain what nodes
		this.idMap = new HashMap<>();
		this.addClustersToIdMap(clusters);
		this.addClustersToIdMap(hubs);
		this.addClustersToIdMap(outliers);
	}

	private void addClustersToIdMap(final Collection<Cluster> clusters) {
		clusters.stream().forEach(c -> c.getMemberIds().stream().forEach(n -> this.idMap.put(n, c.getId())));
	}

	/**
	 * Number of nodes in all the clusters.
	 *
	 * @return the the number of nodes contained in all the clusters
	 */
	public int numNodes() {
		return this.idMap.size();
	}

	/**
	 * Gets the regular clusters.
	 *
	 * @return The regular clusters.
	 */
	public Collection<Cluster> getClusters() {
		return this.clusters;
	}

	/**
	 * Gets the hub clusters.
	 *
	 * @return The hub clusters.
	 */
	public Collection<Cluster> getHubs() {
		return this.hubs;
	}

	/**
	 * Gets the outlier clusters.
	 *
	 * @return The outlier clusters.
	 */
	public Collection<Cluster> getOutliers() {
		return this.outliers;
	}

	/**
	 * Gets the total number of clusters (clusters + hubs + outliers) in this result.
	 *
	 * @return The total number of clusters.
	 */
	public int numClusters() {
		return this.clusters.size() + this.hubs.size() + this.outliers.size();
	}

	/**
	 * Gets a value indicating whether or not the given id is a regular cluster id.
	 *
	 * @param id The id.
	 * @return true if the id is a regular cluster id, false otherwise.
	 */
	public boolean isRegularCluster(final ClusterId id) {
		return contains(this.clusters, id);
	}

	/**
	 * Gets a value indicating whether or not the given id is hub id.
	 *
	 * @param id The id.
	 * @return true if the id is a hub id, false otherwise.
	 */
	public boolean isHub(final ClusterId id) {
		return contains(this.hubs, id);
	}

	/**
	 * Gets a value indicating whether or not the given id is hub id.
	 *
	 * @param id The id.
	 * @return true if the id is a hub id, false otherwise.
	 */
	public boolean isOutlier(final ClusterId id) {
		return contains(this.outliers, id);
	}

	private static boolean contains(final Collection<Cluster> clusters, final ClusterId id) {
		for (final Cluster cluster : clusters) {
			if (cluster.getId().equals(id)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the average cluster size.
	 *
	 * @return The average cluster size.
	 */
	public double getAverageClusterSize() {
		final double numClusterMembers = this.clusters.stream().mapToInt(Cluster::size).sum();
		final int numClusters = this.clusters.size();
		return 0 == numClusters ? 0.0 : numClusterMembers / numClusters;
	}

	/**
	 * Gets the id of the cluster for the given node id.
	 *
	 * @param nodeId The node id.
	 * @return The id of the cluster associated with the specified node.
	 */
	public ClusterId getIdForNode(final NodeId nodeId) {
		return this.idMap.get(nodeId);
	}
}
