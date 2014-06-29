package org.nem.peer.trust;

import org.nem.peer.node.Node;

import java.util.List;

/**
 * Interface for selecting a node.
 */
public interface NodeSelector {

	/**
	 * Selects a node.
	 *
	 * @return The node.
	 */
	public default Node selectNode() {
		final List<Node> nodes = this.selectNodes(1);
		return nodes.size() > 0 ? nodes.get(0) : null;
	}

	/**
	 * Selects at most the specified number of nodes.
	 *
	 * @param maxNodes The maximum number of nodes to select.
	 * @return The nodes.
	 */
	public List<Node> selectNodes(final int maxNodes);
}
