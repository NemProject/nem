package org.nem.peer;

/**
 * Synchronizes the running node's block chain with the another node's block chain.
 */
public interface BlockSynchronizer {

    /**
     * Synchronizes the running node's block chain with node's block chains.
     *
     * @param connector The connector.
     * @param node The other node.
     */
	public void synchronizeNode(final PeerConnector connector, final Node node);
}
