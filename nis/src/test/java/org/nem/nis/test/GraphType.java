package org.nem.nis.test;

/**
 * An enum of well-known graph types.
 *
 * Graph interpretation: i----oj means i has directed edge to j
 */
public enum GraphType {

	/**
	 * <pre>
	 * Graph:         0
	 * </pre>
	 * Clusters: none
	 * Hubs: none
	 * Outliers: {0}
	 */
	GRAPH_SINGLE_NODE,

	/**
	 * <pre>
	 * Graph:         0     1
	 * </pre>
	 * Clusters: none
	 * Hubs: none
	 * Outliers: {0}, {1}
	 */
	GRAPH_TWO_UNCONNECTED_NODES,

	/**
	 * <pre>
	 * Graph:         0----o1
	 * </pre>
	 * Clusters: none
	 * Hubs: none
	 * Outliers: {0}, {1}
	 */
	GRAPH_TWO_CONNECTED_NODES,

	/**
	 * <pre>
	 * Graph:         0----o1----o2----o3----o4
	 * </pre>
	 * Clusters: {0,1,2,3,4}
	 * Hubs: none
	 * Outliers: none
	 */
	GRAPH_LINE_STRUCTURE,

	/**
	 * <pre>
	 * Graph:         0----o1----o2
	 *                o           |
	 *                |           o
	 *                4o----------3
	 * </pre>
	 * Clusters: {0,1,2,3,4}
	 * Hubs: none
	 * Outliers: none
	 */
	GRAPH_RING_STRUCTURE,

	/**
	 * <pre>
	 * Graph:         0---o1
	 *                o\  o|
	 *                | \/ |
	 *                | /\ |
	 *                |/  oo
	 *                3o---2
	 * </pre>
	 * Clusters: {0,1,2,3}
	 * Hubs: none
	 * Outliers: none
	 */
	GRAPH_ONE_CLUSTERS_NO_HUB_NO_OUTLIER,

	/**
	 * <pre>
	 * Graph:         0---o1---o4
	 *                o\  o|
	 *                | \/ |
	 *                | /\ |
	 *                |/  oo
	 *                3o---2
	 * </pre>
	 * Clusters: {0,1,2,3}
	 * Hubs: none
	 * Outliers: {4}
	 */
	GRAPH_ONE_CLUSTERS_NO_HUB_ONE_OUTLIER,

	/**
	 * <pre>
	 * Graph:         0           4
	 *               / o         o \
	 *              o   \       /   o
	 *             1----o2----o3o----5
	 * </pre>
	 * Clusters: {0,1,2}, {3,4,5}
	 * Hubs: none
	 * Outliers: none
	 */
	GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER,

	/**
	 * <pre>
	 * Graph:         0
	 *               / o
	 *              o   \
	 *             1----o2----o6
	 *                   |
	 *                   o
	 *                   3
	 *                  / o
	 *                 o   \
	 *                4----o5
	 * </pre>
	 * Clusters: {0,1,2}, {3,4,5}
	 * Hubs: none
	 * Outliers: {6}
	 */
	GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER,

	/**
	 * <pre>
	 * Graph:         0                 5
	 *               / o               o \
	 *              o   \             /   o
	 *             1----o2----o3----o4o----6
	 * </pre>
	 * Clusters: {0,1,2}, {4,5,6}
	 * Hubs: {3}
	 * Outliers: none
	 */
	GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER,

	/**
	 * <pre>
	 * Graph:         0-------o7-------o5-----o8
	 *               / o               o \
	 *              o   \             /   o
	 *      10o----1----o2----o3----o4o----6
	 *                         |
	 *                         o
	 *                         9
	 * </pre>
	 * Clusters: {0,1,2,10}, {4,5,6}
	 * Hubs: {3}, {7}
	 * Outliers: {8}, {9}
	 */
	GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS
}
