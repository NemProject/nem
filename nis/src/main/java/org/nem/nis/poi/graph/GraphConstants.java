package org.nem.nis.poi.graph;

/**
 * Constants for the clustering algorithm.
 */
public class GraphConstants {

	/**
	 * The minimum number of neighbors with high structural similarity that
	 * a node must have to be considered core.
	 * The node itself is considered as neighbor of itself (it is in its set of similar neighbors).
	 */
	public static final int MU = 3;

	/**
	 * The structural similarity threshold that will cause nodes to be considered
	 * highly similar (if they have a similarity greater than this value).
	 */
	public static final double EPSILON = 0.65;
}
