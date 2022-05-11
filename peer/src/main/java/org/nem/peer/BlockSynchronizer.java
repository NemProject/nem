package org.nem.peer;

import org.nem.core.node.Node;
import org.nem.peer.connect.SyncConnectorPool;

/**
 * Synchronizes the running node's block chain with the another node's block chain.
 */
public interface BlockSynchronizer {

	/**
	 * Synchronizes the running node's block chain with node's block chains.
	 *
	 * @param connector The connector pool.
	 * @param node The other node.
	 * @return The synchronize node result.
	 */
	NodeInteractionResult synchronizeNode(final SyncConnectorPool connector, final Node node);
}
