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

	/**
	 * Creates an importance aware node selector.
	 *
	 * @return An importance aware node selector.
	 */
	public NodeSelector createImportanceAwareNodeSelector();
}