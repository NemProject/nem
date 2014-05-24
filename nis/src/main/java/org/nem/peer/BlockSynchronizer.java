package org.nem.peer;

import org.nem.peer.connect.SyncConnectorPool;
import org.nem.peer.node.Node;

/**
 * Synchronizes the running node's block chain with the another node's block chain.
 */
public interface BlockSynchronizer {

	/**
	 * Synchronizes the running node's block chain with node's block chains.
	 *
	 * @param connector The connector pool.
	 * @param node The other node.
	 * @return true if the sync succeeded; false if there was a fatal error.
	 */
	public boolean synchronizeNode(final SyncConnectorPool connector, final Node node);
}
