package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.NodeId;

import java.util.ArrayList;

/**
 * Trivial clustering: Do not scan at all but build one outlier cluster for each node.
 */
public class OutlierScan implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final ArrayList<Cluster> clusters = new ArrayList<>();
		for (int i = 0; i < neighborhood.size(); ++i) {
			final Cluster cluster = new Cluster(new NodeId(i));
			clusters.add(cluster);
		}

		return new ClusteringResult(new ArrayList<>(), new ArrayList<>(), clusters);
	}
}
