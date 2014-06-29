package org.nem.peer;

import org.nem.peer.trust.NodeSelector;

/**
 * Factory interface for creating a node selector.
 */
public interface NodeSelectorFactory {

	/**
	 * Creates a node selector.
	 *
	 * @return A node selector.
	 */
	public NodeSelector createNodeSelector();
}