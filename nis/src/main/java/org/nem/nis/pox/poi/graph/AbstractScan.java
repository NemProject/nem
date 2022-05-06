package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;

/**
 * Abstract base class shared by Scan and FastScan implementations
 */
public abstract class AbstractScan {
	/**
	 * Special cluster id that indicates the node is not part of any cluster.
	 */
	private static final int NON_MEMBER_CLUSTER_ID = -1;

	private final Neighborhood neighborhood;
	private final Integer neighborhoodSize;
	private final Integer[] nodeStates;
	private final List<Cluster> clusters = new ArrayList<>();
	private final List<Cluster> hubs = new ArrayList<>();
	private final List<Cluster> outliers = new ArrayList<>();

	/**
	 * Creates a new abstract scan.
	 *
	 * @param neighborhood The neighborhood.
	 */
	public AbstractScan(final Neighborhood neighborhood) {
		this.neighborhood = neighborhood;
		this.neighborhoodSize = this.neighborhood.size();
		this.nodeStates = new Integer[this.neighborhoodSize];
	}

	/**
	 * Actually performs the scan.
	 */
	public void scan() {
		this.findClusters();
		this.processNonMembers();
	}

	/**
	 * Builds a cluster around the specified community. <em>This function is only called for unclassified core communities.</em>
	 *
	 * @param community The community to cluster around.
	 */
	protected abstract void cluster(final Community community);

	private void findClusters() {
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
			this.cluster(community);
		}
	}

	private void processNonMembers() {
		for (int i = 0; i < this.neighborhoodSize; ++i) {
			if (NON_MEMBER_CLUSTER_ID != this.nodeStates[i]) {
				continue;
			}

			final NodeId nodeId = new NodeId(i);
			final Cluster cluster = new Cluster(nodeId);
			(this.isHub(this.neighborhood.getCommunity(nodeId)) ? this.hubs : this.outliers).add(cluster);
		}
	}

	private boolean isHub(final Community community) {
		final HashSet<ClusterId> connectedClusterIds = new HashSet<>();
		return this.isHub(connectedClusterIds, community.getDissimilarNeighbors())
				|| this.isHub(connectedClusterIds, community.getSimilarNeighbors());
	}

	private boolean isHub(final HashSet<ClusterId> connectedClusterIds, final NodeNeighbors neighbors) {
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

	/**
	 * Marks a community as non-core.
	 *
	 * @param community The community to mark as non-core.
	 */
	protected void markAsNonCore(final Community community) {
		final int pivotId = community.getPivotId().getRaw();
		if (null == this.nodeStates[pivotId]) {
			this.nodeStates[pivotId] = NON_MEMBER_CLUSTER_ID;
		}
	}

	// region getters / setters

	/**
	 * Gets the neighborhood.
	 *
	 * @return The neighborhood.
	 */
	protected Neighborhood getNeighborhood() {
		return this.neighborhood;
	}

	/**
	 * Gets the state for the specified node id.
	 *
	 * @param id The node id.
	 * @return The cluster id.
	 */
	protected Integer getNodeState(final NodeId id) {
		return this.nodeStates[id.getRaw()];
	}

	/**
	 * Sets the state for the specified node id.
	 *
	 * @param id The node id.
	 * @param clusterId The cluster id.
	 */
	protected void setNodeState(final NodeId id, final ClusterId clusterId) {
		this.nodeStates[id.getRaw()] = clusterId.getRaw();
	}

	/**
	 * Gets a value indicating whether or not the specified node is part of a cluster.
	 *
	 * @param id The node id.
	 * @return true if the specified node is part of a cluster.
	 */
	protected boolean isClustered(final NodeId id) {
		final Integer state = this.getNodeState(id);
		return null != state && NON_MEMBER_CLUSTER_ID != state;
	}

	// endregion

	// region cluster functions

	/**
	 * Adds the specified cluster.
	 *
	 * @param cluster The cluster.
	 */
	protected void addCluster(final Cluster cluster) {
		this.clusters.add(cluster);
	}

	/**
	 * Removes the specified cluster.
	 *
	 * @param cluster The cluster.
	 */
	private void removeCluster(final Cluster cluster) {
		this.clusters.remove(cluster);
	}

	/**
	 * Returns the cluster with a given id from the cluster collection.
	 *
	 * @param clusterId The id for the cluster.
	 * @return The cluster with the wanted id.
	 */
	private Cluster findCluster(final ClusterId clusterId) {
		// a linear search is ok because the number of expected clusters is small (1000-5000)
		for (final Cluster cluster : this.clusters) {
			if (cluster.getId().equals(clusterId)) {
				return cluster;
			}
		}

		throw new IllegalArgumentException("cluster with id " + clusterId + " not found.");
	}

	/**
	 * Merge existing clusters.
	 *
	 * @param clusterIds The ids of the clusters to merge
	 * @return The merged cluster.
	 */
	protected Cluster mergeClusters(final List<ClusterId> clusterIds) {
		if (clusterIds.size() <= 0) {
			throw new IllegalArgumentException("need at least one cluster id to merge");
		}

		final ClusterId clusterId = clusterIds.get(0);
		final Cluster cluster = this.findCluster(clusterId);

		for (int ndx = 1; ndx < clusterIds.size(); ++ndx) {
			final Cluster clusterToMerge = this.findCluster(clusterIds.get(ndx));
			cluster.merge(clusterToMerge);
			clusterToMerge.getMemberIds().stream().forEach(id -> this.setNodeState(id, clusterId));
			this.removeCluster(clusterToMerge);
		}

		return cluster;
	}

	// endregion
}
