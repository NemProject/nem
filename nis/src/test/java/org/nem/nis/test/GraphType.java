package org.nem.nis.test;

/**
 * An enum of well-known graph types.
 * <br/>
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
	GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS,

	// TODO 20141019: most of the above graphs are no longer correct after changing the poi parameters
	// > fixing them leads to graphs like the following; not sure if that's the best use of time:

	/**
	 * 	/**
	 * <pre>
	 * Graph:         0    14
	 *               / \   o
	 *              o   o /
	 *             1----o4----o10
	 *
	 *                2    15
	 *               o o   o
	 *               |  \ /
	 *              9o---3---o7    16---o3,4,5o---18
	 *                              |             |
	 *                              o             o
	 *             12o---5---o11   17             19
	 *                  / \
	 *                 o   o         13
	 *                8----o6
	 * <br/>
	 * sim(16, 3) = (|{16,3}|)/sqrt(5*6) = 2/sqrt(30) = 0.37
	 * sim(16, 4) = (|{16,4}|)/sqrt(5*6) = 2/sqrt(30) = 0.37
	 * sim(16, 5) = (|{16,5}|)/sqrt(5*6) = 2/sqrt(30) = 0.37
	 * </pre>
	 * Expected:
	 * Clusters {0, 1, 4, 10, 14}, {2, 3, 7, 9, 15}, {5, 6, 8, 11, 12}
	 * Hubs: {16}, {18}
	 * Outliers: {13}, {17}, {19}
	 */
	GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS
}
