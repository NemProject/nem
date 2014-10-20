package org.nem.nis.test;

import org.nem.core.model.primitive.*;
import org.nem.nis.poi.graph.*;

import java.util.*;

/**
 * Static factory class that exposes functions for creating expected clusters for well-known graph types.
 */
public class IdealizedClusterFactory {

	/**
	 * Creates an clustering result representing the specified graph type.
	 *
	 * @param graphType The graph type.
	 * @return The clustering result.
	 */
	public static ClusteringResult create(final GraphType graphType) {
		final Collection<Cluster> clusters = new ArrayList<>();
		final Collection<Cluster> hubs = new ArrayList<>();
		final Collection<Cluster> outliers = new ArrayList<>();
		switch (graphType) {
			case GRAPH_SINGLE_NODE:
				outliers.add(new Cluster(new NodeId(0)));
				break;

			case GRAPH_TWO_UNCONNECTED_NODES:
			case GRAPH_TWO_CONNECTED_NODES:
				outliers.add(new Cluster(new NodeId(0)));
				outliers.add(new Cluster(new NodeId(1)));
				break;

			case GRAPH_LINE_STRUCTURE:
			case GRAPH_RING_STRUCTURE:
				clusters.add(new Cluster(new ClusterId(1), NisUtils.toNodeIdList(0, 1, 2, 3, 4)));
				break;

			case GRAPH_ONE_CLUSTERS_NO_HUB_NO_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				break;

			case GRAPH_ONE_CLUSTERS_NO_HUB_ONE_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				outliers.add(new Cluster(new NodeId(4)));
				break;

			case GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2)));
				clusters.add(new Cluster(new ClusterId(3), NisUtils.toNodeIdList(3, 4, 5)));
				break;

			case GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2)));
				clusters.add(new Cluster(new ClusterId(3), NisUtils.toNodeIdList(3, 4, 5)));
				outliers.add(new Cluster(new NodeId(6)));
				break;

			case GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6)));
				hubs.add(new Cluster(new NodeId(3)));
				break;

			case GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 10)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6)));
				hubs.add(new Cluster(new NodeId(3)));
				hubs.add(new Cluster(new NodeId(7)));
				outliers.add(new Cluster(new NodeId(8)));
				outliers.add(new Cluster(new NodeId(9)));
				break;

			case GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 4, 10, 14)));
				clusters.add(new Cluster(new ClusterId(2), NisUtils.toNodeIdList(2, 3, 7, 9, 15)));
				clusters.add(new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 8, 11 ,12)));
				hubs.add(new Cluster(new NodeId(16)));
				hubs.add(new Cluster(new NodeId(18)));
				outliers.add(new Cluster(new NodeId(13)));
				outliers.add(new Cluster(new NodeId(17)));
				outliers.add(new Cluster(new NodeId(19)));
				break;

			default: {
				throw new IllegalArgumentException("unknown graph");
			}
		}

		return new ClusteringResult(clusters, hubs, outliers);
	}
}
