package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.ArrayList;

/**
 * Trivial clustering: Do not scan at all but build one outlier cluster for each node.
 */
public class OutlierScan implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final ArrayList<Cluster> clusters = new ArrayList<>();
		Cluster cluster;
		if (neighborhood.size() > 0) {
			for (int i = 0; i < neighborhood.size(); ++i) {
				cluster = new Cluster(new ClusterId(i));
				cluster.add(new NodeId(i));
				clusters.add(cluster);
			}
		}

		return new ClusteringResult(new ArrayList<>(), new ArrayList<>(), clusters);
	}
}
