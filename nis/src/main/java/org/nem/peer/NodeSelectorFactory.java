package org.nem.peer;

import org.nem.peer.trust.NodeSelector;

/**
 * Factory interface for creating node selectors.
 */
public interface NodeSelectorFactory {

	/**
	 * Creates a node selector.
	 *
	 * @return A node selector.
	 */
	public NodeSelector createNodeSelector();

	// TODO 20140909 B-J as mentioned earlier i don't think this should be in the interface
	/**
	 * Creates an importance aware node selector.
	 *
	 * @return An importance aware node selector.
	 */
	public NodeSelector createImportanceAwareNodeSelector();
}