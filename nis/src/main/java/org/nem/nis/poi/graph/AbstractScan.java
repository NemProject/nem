package org.nem.nis.poi.graph;

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

	// TODO: these are protected fields for performance, but not good for design
	protected final Neighborhood neighborhood;
	private final Integer neighborhoodSize;
	protected final Integer[] nodeStates;
	protected final List<Cluster> clusters = new ArrayList<>();

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
	 * Builds a cluster around the specified community.
	 * <em>This function is only called for unclassified core communities.</em>
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
			cluster(community);
		}
	}

	private void processNonMembers() {
		for (int i = 0; i < this.neighborhoodSize; ++i) {
			if (NON_MEMBER_CLUSTER_ID != this.nodeStates[i]) {
				continue;
			}

			final Cluster cluster = new Cluster(new ClusterId(i));
			cluster.add(new NodeId(i));
			(this.isHub(this.neighborhood.getCommunity(new NodeId(i))) ? this.hubs : this.outliers).add(cluster);
		}
	}

	private boolean isHub(final Community community) {
		final HashSet<ClusterId> connectedClusterIds = new HashSet<>();
		return isHub(connectedClusterIds, community.getSimilarNeighbors()) ||
				isHub(connectedClusterIds, community.getDissimilarNeighbors());
	}

	private boolean isHub(final HashSet<ClusterId> connectedClusterIds, final NodeNeighbors neighbors) {
		// TODO 20141001 J-M since our graph is bidirectional, can there ever be a case where
		// > a hub can has two similar neighbors (i can't think of one)?
		// TODO 20141001 J-M: It depends on GraphConstants.MU.
		// TODO :) i meant when MU is 3 (like it is now)
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

	/**
	 * Gets a value indicating whether or not the specified node is part of a cluster.
	 *
	 * @param id The node id.
	 * @return true if the specified node is part of a cluster.
	 */
	protected boolean isClustered(final int id) {
		return null != this.nodeStates[id] && NON_MEMBER_CLUSTER_ID != this.nodeStates[id];
	}

	/**
	 * Returns the cluster with a given id from the cluster collection.
	 *
	 * @param clusterId The id for the cluster.
	 * @return The cluster with the wanted id.
	 */
	protected Cluster findCluster(final ClusterId clusterId) {
		// TODO 20141002 i guess since the number of expected clusters is small, a linear search is ok here
		for (final Cluster cluster : this.clusters) {
			if (cluster.getId().equals(clusterId)) {
				return cluster;
			}
		}

		throw new IllegalArgumentException("cluster with id " + clusterId + " not found.");
	}
}
