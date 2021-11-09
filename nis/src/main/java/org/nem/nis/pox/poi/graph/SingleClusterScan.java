package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.ArrayList;

/**
 * Trivial clustering: Do not scan at all but build one cluster with all nodes in it. Using this clusterer the NCD-aware algorithm equals
 * the original PR algorithm.
 */
public class SingleClusterScan implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final ArrayList<Cluster> clusters = new ArrayList<>();
		final ClusterId clusterId = new ClusterId(0);
		if (neighborhood.size() > 0) {
			final Cluster cluster = new Cluster(clusterId);
			for (int i = 0; i < neighborhood.size(); ++i) {
				cluster.add(new NodeId(i));
			}

			clusters.add(cluster);
		}

		return new ClusteringResult(clusters, new ArrayList<>(), new ArrayList<>());
	}
}
