package org.nem.peer.services;

import org.nem.core.node.NodeCollection;

import java.util.logging.Logger;

/**
 * Helper class used to implement inactive node pruning.
 */
public class InactiveNodePruner {
	private static final Logger LOGGER = Logger.getLogger(InactiveNodePruner.class.getName());

	/**
	 * Prunes the nodes in the specified collection.
	 *
	 * @param nodes The node collection.
	 * @return The number of noes that were pruned.
	 */
	public int prune(final NodeCollection nodes) {
		LOGGER.fine("pruning inactive nodes");
		final int numInitialNodes = nodes.size();
		nodes.prune();
		final int numNodesPruned = numInitialNodes - nodes.size();
		LOGGER.info(String.format("%d inactive node(s) were pruned", numNodesPruned));
		return numNodesPruned;
	}
}
