package org.nem.nis.test;

/**
 * An enum of well-known graph types where the epsilon value doesn't matter because the graphs are either fully connected or unconnected.
 * <br>
 * Graph interpretation: i----oj means i has directed edge to j
 */
public enum GraphType {

	/**
	 * <pre>
	 * Graph:         0
	 * </pre>
	 *
	 * Clusters: none Hubs: none Outliers: {0}
	 */
	GRAPH_SINGLE_NODE,

	/**
	 * <pre>
	 * Graph:         0     1
	 * </pre>
	 *
	 * Clusters: none Hubs: none Outliers: {0}, {1}
	 */
	GRAPH_TWO_UNCONNECTED_NODES,

	/**
	 * <pre>
	 * Graph:         0----o1
	 * </pre>
	 *
	 * Clusters: none Hubs: none Outliers: {0}, {1}
	 */
	GRAPH_TWO_CONNECTED_NODES,

	/**
	 * <pre>
	 * Graph:         0----o1----o2----o3----o4
	 * </pre>
	 *
	 * Clusters: {0,1,2,3,4} Hubs: none Outliers: none
	 */
	GRAPH_LINE_STRUCTURE,

	/**
	 * <pre>
	 * Graph:         0--o1--o2--o3--o4--o5
	 * </pre>
	 *
	 * Clusters: {0,1,2,3,4,5} Hubs: none Outliers: none
	 */
	GRAPH_LINE6_STRUCTURE,

	/**
	 * <pre>
	 * Graph:         0----o1----o2
	 *                o           |
	 *                |           o
	 *                4o----------3
	 * </pre>
	 *
	 * Clusters: {0,1,2,3,4} Hubs: none Outliers: none
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
	 *
	 * Clusters: {0,1,2,3} Hubs: none Outliers: none
	 */
	GRAPH_BOX_TWO_DIAGONALS,

	/**
	 * <pre>
	 * First Graph: 0 --- 1
	 *              | \   |
	 *              |   \ |
	 *              3 --- 2
	 * <br>
	 * Similarities:
	 *        sim(0,1) = (1+1+1)/sqrt(4*3) = sqrt(3/4)
	 *                 = sim(1,0)
	 *                 = sim(0,3) = sim(3,0)
	 *                 = sim(2,3) = sim(3,2)
	 *                 = sim(1,2) = sim(2,1) > EPSILON
	 *        sim(0,2) = (2+1+1)/sqrt(4*4) = 1
	 *                 = sim(2,0) > EPSILON
	 *        sim(1,3) = (2+0+0)/sqrt(3*3) = 2/3
	 *                 = sim(3,1) < EPSILON
	 * <br>
	 * Communities in form (node id, epsilon neighbors, non-epsilon neighbors):
	 *        com(0) = (0, {1,2,3}, {})
	 *        com(1) = (1, {0,2}, {})
	 *        com(2) = (2, {0,1,3}, {})
	 *        com(3) = (3, {0,2}, {})
	 * </pre>
	 *
	 * Clusters: {0,1,2,3} Hubs: none Outliers: none
	 */
	GRAPH_BOX_MAJOR_DIAGONAL,

	/**
	 * <pre>
	 * First Graph: 0 --- 1 (essentially the same graph as GRAPH_BOX_MAJOR_DIAGONAL
	 * (isomorphic) |   / | but we start scanning from a different node)
	 *              | /   |
	 *              3 --- 2
	 * <br>
	 * Similarities:
	 *        sim(0,1) = (1+1+1)/sqrt(3*4) = sqrt(3/4)
	 *                 = sim(1,0)
	 *                 = sim(0,3) = sim(3,0)
	 *                 = sim(2,3) = sim(3,2)
	 *                 = sim(1,2) = sim(2,1) > EPSILON
	 *        sim(0,2) = (2+0+0)/sqrt(3*3) = 2/3
	 *                 = sim(2,0) < EPSILON
	 *        sim(1,3) = (2+1+1)/sqrt(4*4) = 1
	 *                 = sim(3,1) > EPSILON
	 * <br>
	 * Communities in form (node id, epsilon neighbors, non-epsilon neighbors):
	 *        com(0) = (0, {1,3}, {})
	 *        com(1) = (1, {0,2,3}, {})
	 *        com(2) = (2, {1,3}, {})
	 *        com(3) = (3, {0,1,2}, {})
	 * </pre>
	 *
	 * Clusters: {0,1,2,3} Hubs: none Outliers: none
	 */
	GRAPH_BOX_MINOR_DIAGONAL,

	/**
	 * <pre>
	 * Graph:      0
	 *            /|\
	 *           / | \
	 *          o  o  o
	 *         1   2   3
	 * </pre>
	 *
	 * Clusters: {0,1,2,3} Hubs: none Outliers: none
	 */
	GRAPH_TREE_STRUCTURE,

	/**
	 * <pre>
	 * Graph:         0---o1
	 *                o\  o|
	 *                | \/ |
	 *                | /\ |
	 *                |/  oo
	 *                3o---2
	 * <br>
	 *                4
	 *                |
	 *                o
	 *         8o-----5----o6
	 *                |
	 *                o
	 *                7
	 * </pre>
	 *
	 * Clusters: {0,1,2,3}, {4,5,6,7,8} Hubs: none Outliers: none
	 */
	GRAPH_DISCONNECTED_BOX_WITH_DIAGONAL_AND_CROSS
}
