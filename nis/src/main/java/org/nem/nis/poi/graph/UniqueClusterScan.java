package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.ArrayList;

/**
 * Trivial clustering: Do not scan at all but build one clusters with all nodes in it.
 * Using this clusterer the NCD-aware algorithm equals the original PR algorithm.
 */
public class UniqueClusterScan implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final ArrayList<Cluster> clusters = new ArrayList<>();
		Cluster cluster;
		if (neighborhood.size() > 0) {
			cluster = new Cluster(new ClusterId(0));
			for (int i = 0; i < neighborhood.size(); ++i) {
				cluster.add(new NodeId(i));
			}
			clusters.add(cluster);
		}

		ClusteringResult result = new ClusteringResult(clusters, new ArrayList<>(), new ArrayList<>());
		System.out.println(result.getClusters().size() + " clusters, " + result.getHubs().size() + " hubs, and " + result.getOutliers().size() + " outliers");

		return result;
	}
}
