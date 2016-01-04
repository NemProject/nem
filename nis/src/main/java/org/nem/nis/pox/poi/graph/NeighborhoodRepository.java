package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * A repository for retrieving a node's neighbors.
 */
public interface NeighborhoodRepository {

	/**
	 * Gets the node ids of the neighbors of the specified node.
	 *
	 * @param nodeId The node id.
	 * @return The node's neighbors.
	 */
	NodeNeighbors getNeighbors(final NodeId nodeId);

	/**
	 * Gets the logical size of the map (the actual size might be smaller if the map is very sparse).
	 *
	 * @return The logical size of the map.
	 */
	int getLogicalSize();
}
