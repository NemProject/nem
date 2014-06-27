package org.nem.peer.trust;

import org.nem.peer.trust.score.NodeExperiencePair;

import java.util.List;

/**
 * Interface for selecting a node.
 */
public interface NodeSelector {

	/**
	 * Selects a node.
	 *
	 * @return Information about the selected node or null if
	 * no suitable nodes could be found.
	 */
	public default NodeExperiencePair selectNode() {
		final List<NodeExperiencePair> nodePairs = this.selectNodes(1);
		return nodePairs.size() > 0 ? nodePairs.get(0) : null;
	}

	/**
	 * Selects at most the specified number of nodes.
	 *
	 * @param maxNodes The maximum number of nodes to select.
	 * @return Information about the selected nodes.
	 */
	public List<NodeExperiencePair> selectNodes(final int maxNodes);
}
