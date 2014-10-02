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
	protected static final int NON_MEMBER_CLUSTER_ID = -1;

	// TODO: these are protected fields for performance, but not good for design
	protected final Neighborhood neighborhood;
	protected final Integer neighborhoodSize;
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
	 * Actually finds the clusters.
	 */
	protected abstract void findClusters();

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
}
