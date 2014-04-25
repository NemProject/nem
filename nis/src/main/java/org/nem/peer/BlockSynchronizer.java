package org.nem.peer;

import org.nem.core.connect.SyncConnectorPool;
import org.nem.peer.node.Node;

/**
 * Synchronizes the running node's block chain with the another node's block chain.
 */
public interface BlockSynchronizer {

	/**
	 * Synchronizes the running node's block chain with node's block chains.
	 *
	 * @param connector The connector pool.
	 * @param node      The other node.
	 */
	public void synchronizeNode(final SyncConnectorPool connector, final Node node);
}
