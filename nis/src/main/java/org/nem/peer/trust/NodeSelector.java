package org.nem.peer.trust;

import org.nem.peer.trust.score.NodeExperiencePair;

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
	public NodeExperiencePair selectNode();
}
