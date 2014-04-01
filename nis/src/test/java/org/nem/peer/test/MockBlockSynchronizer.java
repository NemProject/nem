package org.nem.peer.test;

import org.nem.peer.BlockSynchronizer;
import org.nem.peer.Node;
import org.nem.peer.PeerConnector;

/**
 * A mock BlockSynchronizer implementation.
 */
public class MockBlockSynchronizer implements BlockSynchronizer {

    private int numSynchronizeNodeCalls;
    private PeerConnector lastConnector;

    /**
     * Gets the number of times synchronizeNode was called.
     *
     * @return The number of times synchronizeNode was called.
     */
    public int getNumSynchronizeNodeCalls() { return this.numSynchronizeNodeCalls; }

    /**
     * Gets the last PeerConnector passed to synchronizeNode.
     *
     * @return The last PeerConnector passed to synchronizeNode.
     */
    public PeerConnector getLastConnector() { return this.lastConnector; }

	@Override
	public void synchronizeNode(final PeerConnector connector, final Node node) {
		++this.numSynchronizeNodeCalls;
        this.lastConnector = connector;
	}
}
