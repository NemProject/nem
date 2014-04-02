package org.nem.peer.test;

import org.nem.peer.BlockSynchronizer;
import org.nem.peer.Node;
import org.nem.peer.SyncConnector;

/**
 * A mock BlockSynchronizer implementation.
 */
public class MockBlockSynchronizer implements BlockSynchronizer {

	private int numSynchronizeNodeCalls;
	private SyncConnector lastConnector;

	/**
	 * Gets the number of times synchronizeNode was called.
	 *
	 * @return The number of times synchronizeNode was called.
	 */
	public int getNumSynchronizeNodeCalls() {
		return this.numSynchronizeNodeCalls;
	}

	/**
	 * Gets the last SyncConnector passed to synchronizeNode.
	 *
	 * @return The last SyncConnector passed to synchronizeNode.
	 */
	public SyncConnector getLastConnector() {
		return this.lastConnector;
	}

	@Override
	public void synchronizeNode(final SyncConnector connector, final Node node) {
		++this.numSynchronizeNodeCalls;
		this.lastConnector = connector;
	}
}
