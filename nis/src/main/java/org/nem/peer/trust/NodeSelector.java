package org.nem.peer.trust;

/**
 * Interface for selecting a node.
 */
public interface NodeSelector {

	/**
	 * Selects a node.
	 *
	 * @param context The trust context.
	 *
	 * @return Information about the selected node.
	 */
	public NodeExperiencePair selectNode(final TrustContext context);
}
