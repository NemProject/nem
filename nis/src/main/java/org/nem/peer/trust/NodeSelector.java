package org.nem.peer.trust;

import org.nem.core.node.Node;

import java.util.List;

/**
 * Interface for selecting a node.
 */
public interface NodeSelector {

	/**
	 * Selects a single node.
	 *
	 * @return The node.
	 */
	public Node selectNode();

	/**
	 * Selects multiple nodes.
	 *
	 * @return The nodes.
	 */
	public List<Node> selectNodes();
}
