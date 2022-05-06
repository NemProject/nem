package org.nem.nis.test;

/**
 * An enum of well-known graph types where epsilon is assumed to have a value of 0.40. <br>
 * Graph interpretation: i----oj means i has directed edge to j
 */
public enum GraphTypeEpsilon040 {

	/**
	 * <pre>
	 *                                                9  8
	 *                   6 7 8 9                       \ |
	 *                   \_\/_/                      6---1---2--3
	 *                 /  \|/ \                        / \   | /|\
	 * Second graph:  0----1   5   is equivalent to   7  \   |/ | \
	 *                |\    \ / \                         \  5  /  \
	 *                | \    2  /                          \ | /   |
	 *                |  \  /  \                            \|/    |
	 *                4----3--/ 8                            0-----4
	 * <br>
	 * Similarities:
	 *        sim(0,1) = (|{0,1}|)/sqrt(5*7) = 2/sqrt(35) ~ 0.3381
	 *                 = sim(1,0) < EPSILON
	 *        sim(0,3) = (|{0,3,4,5}|)/sqrt(5*5) = 4/5
	 *                 = sim(3,0) > EPSILON
	 *        sim(0,4) = (|{0,4,3}|)/sqrt(5*3) = 3/sqrt(15) ~ 0.7746
	 *                 = sim(4,0) > EPSILON
	 *        sim(0,5) = (|{0,3,5}|)/sqrt(4*5) = 3/sqrt(20) ~ 0.6708
	 *                 = sim(5,0) < EPSILON
	 *        sim(1,2) = (|{1,2}|)/sqrt(7*4) = 2/sqrt(28) ~ 0.3780
	 *                 = sim(2,1) < EPSILON
	 *        sim(2,3) = (|{2,3,5}|)/sqrt(4*5) = 3/sqrt(20) ~ 0.6708
	 *                 = sim(3,2) < EPSILON
	 *        sim(2,5) = (|{2,3,5}|)/sqrt(4*4) = 3/4
	 *                 = sim(5,2) > EPSILON
	 *        sim(3,4) = (|{0,3,4}|)/sqrt(5*3) = 3/sqrt(15) ~ 0.7746
	 *                 = sim(4,3) > EPSILON
	 *        sim(3,5) = (|{0,2,3,5}|)/sqrt(5*4) = 4/sqrt(20) ~ 0.8944
	 *                 = sim(5,3) > EPSILON
	 * <br>
	 * Communities in form (node id, similar neighbors, dissimilar neighbors):
	 *         com(0) = (0, {3,4}, {1,5})
	 *         com(1) = (1, {}, {0,2})
	 *         com(2) = (2, {5}, {1,3})
	 *         com(3) = (3, {0,4,5}, {2})
	 *         com(4) = (4, {0,3}, {})
	 *         com(5) = (5, {2,3}, {0})
	 * <br>
	 * Expected:
	 * - clusters: {0,2,3,4,5}, {1,6,7,8,9}
	 * - hubs: none
	 * - outliers: none
	 * </pre>
	 */
	GRAPH_TWO_CLUSTERS_NO_HUBS_NO_OUTLIERS,

	/**
	 * <pre>
	 * Graph:        0---11--15
	 *              / \  |  /
	 *             /   \ | /
	 *             1-----2----7----16
	 *             |   / | \
	 *              10 --|--14
	 *                   |
	 *                   3----17
	 *                   |
	 *               13--|--12
	 *              /  \ | / \
	 *              9----4----8
	 *               \  / \  /
	 *                5-----6
	 * <br>
	 * Expected:
	 * - clusters: {0, 1, 2, 10, 11, 14, 15}, {4, 5, 6, 8, 9, 12, 13}
	 * - hubs {3}
	 * - outliers {7}, {16}, {17}
	 * </pre>
	 */
	GRAPH_TWO_CLUSTERS_ONE_HUB_THREE_OUTLIERS,

	/**
	 * <pre>
	 * Graph:                               0    14
	 *                                     / \   o
	 *                                    o   o /
	 *                                   1----o4----o10
	 *                                        o
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
	 * <br>
	 * sim(16, 3) = (|{16,3}|)/sqrt(5*6) = 2/sqrt(30) = 0.37
	 * sim(16, 4) = (|{16,4}|)/sqrt(5*6) = 2/sqrt(30) = 0.37
	 * sim(16, 5) = (|{16,5}|)/sqrt(5*6) = 2/sqrt(30) = 0.37
	 * </pre>
	 *
	 * Expected:<br>
	 * Clusters {0, 1, 4, 10, 14}, {2, 3, 7, 9, 15}, {5, 6, 8, 11, 12} Hubs: {16}, {18} Outliers: {13}, {17}, {19}
	 */
	GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS
}
