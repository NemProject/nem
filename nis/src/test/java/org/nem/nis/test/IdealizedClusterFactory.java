package org.nem.nis.test;

import org.nem.core.model.primitive.*;
import org.nem.nis.pox.poi.graph.*;

import java.util.*;
import java.util.stream.IntStream;

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
				IntStream.range(0, 5).forEach(i -> outliers.add(new Cluster(new NodeId(i))));
				break;

			case GRAPH_LINE6_STRUCTURE:
				IntStream.range(0, 6).forEach(i -> outliers.add(new Cluster(new NodeId(i))));
				break;

			case GRAPH_BOX_TWO_DIAGONALS:
			case GRAPH_BOX_MAJOR_DIAGONAL:
			case GRAPH_BOX_MINOR_DIAGONAL:
			case GRAPH_TREE_STRUCTURE:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				break;

			case GRAPH_DISCONNECTED_BOX_WITH_DIAGONAL_AND_CROSS:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6, 7, 8)));
				break;

			default : {
				throw new IllegalArgumentException("unknown graph");
			}
		}

		return new ClusteringResult(clusters, hubs, outliers);
	}

	/**
	 * Creates an clustering result representing the specified graph type.
	 *
	 * @param graphType The graph type.
	 * @return The clustering result.
	 */
	public static ClusteringResult create(final GraphTypeEpsilon065 graphType) {
		final Collection<Cluster> clusters = new ArrayList<>();
		final Collection<Cluster> hubs = new ArrayList<>();
		final Collection<Cluster> outliers = new ArrayList<>();
		switch (graphType) {
			case GRAPH_ONE_CLUSTER_NO_HUB_ONE_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				outliers.add(new Cluster(new NodeId(4)));
				break;

			case GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6, 7)));
				break;

			case GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6, 7)));
				outliers.add(new Cluster(new NodeId(8)));
				break;

			case GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6, 7)));
				hubs.add(new Cluster(new NodeId(8)));
				break;

			case GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 12)));
				clusters.add(new Cluster(new ClusterId(4), NisUtils.toNodeIdList(4, 5, 6, 7)));
				hubs.add(new Cluster(new NodeId(8)));
				hubs.add(new Cluster(new NodeId(9)));
				outliers.add(new Cluster(new NodeId(10)));
				outliers.add(new Cluster(new NodeId(11)));
				break;

			default : {
				throw new IllegalArgumentException("unknown graph");
			}
		}

		return new ClusteringResult(clusters, hubs, outliers);
	}

	/**
	 * Creates an clustering result representing the specified graph type.
	 *
	 * @param graphType The graph type.
	 * @return The clustering result.
	 */
	public static ClusteringResult create(final GraphTypeEpsilon040 graphType) {
		final Collection<Cluster> clusters = new ArrayList<>();
		final Collection<Cluster> hubs = new ArrayList<>();
		final Collection<Cluster> outliers = new ArrayList<>();
		switch (graphType) {
			case GRAPH_TWO_CLUSTERS_NO_HUBS_NO_OUTLIERS:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 2, 3, 4, 5)));
				clusters.add(new Cluster(new ClusterId(1), NisUtils.toNodeIdList(1, 6, 7, 8, 9)));
				break;

			case GRAPH_TWO_CLUSTERS_ONE_HUB_THREE_OUTLIERS:
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 10, 11, 14, 15)));
				clusters.add(new Cluster(new ClusterId(1), NisUtils.toNodeIdList(4, 5, 6, 8, 9, 12, 13)));
				hubs.add(new Cluster(new NodeId(3)));
				outliers.add(new Cluster(new NodeId(7)));
				outliers.add(new Cluster(new NodeId(16)));
				outliers.add(new Cluster(new NodeId(17)));
				break;

			case GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS:
				clusters.add(new Cluster(new ClusterId(2), NisUtils.toNodeIdList(2, 3, 7, 9, 15)));
				clusters.add(new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 4, 10, 14)));
				clusters.add(new Cluster(new ClusterId(5), NisUtils.toNodeIdList(5, 6, 8, 11, 12)));
				hubs.add(new Cluster(new NodeId(16)));
				hubs.add(new Cluster(new NodeId(18)));
				outliers.add(new Cluster(new NodeId(13)));
				outliers.add(new Cluster(new NodeId(17)));
				outliers.add(new Cluster(new NodeId(19)));
				break;

			default : {
				throw new IllegalArgumentException("unknown graph");
			}
		}

		return new ClusteringResult(clusters, hubs, outliers);
	}
}
