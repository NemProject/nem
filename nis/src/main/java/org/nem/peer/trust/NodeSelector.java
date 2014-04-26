package org.nem.peer.trust;

import org.nem.peer.trust.score.NodeExperiencePair;

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
