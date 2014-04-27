package org.nem.peer.test;

import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;
import org.nem.peer.node.Node;

/**
 * A mock BlockSynchronizer implementation.
 */
public class MockBlockSynchronizer implements BlockSynchronizer {

	private int numSynchronizeNodeCalls;
	private SyncConnectorPool lastConnectorPool;
	private Node lastNode;

	/**
	 * Gets the number of times synchronizeNode was called.
	 *
	 * @return The number of times synchronizeNode was called.
	 */
	public int getNumSynchronizeNodeCalls() {
		return this.numSynchronizeNodeCalls;
	}

	/**
	 * Gets the last SyncConnectorPool passed to synchronizeNode.
	 *
	 * @return The last SyncConnectorPool passed to synchronizeNode.
	 */
	public SyncConnectorPool getLastConnectorPool() {
		return this.lastConnectorPool;
	}

	/**
	 * Gets the last Node passed to synchronizeNode.
	 *
	 * @return The last Node passed to synchronizeNode.
	 */
	public Node getLastNode() {
		return this.lastNode;
	}

	@Override
	public void synchronizeNode(final SyncConnectorPool connectorPool, final Node node) {
		++this.numSynchronizeNodeCalls;
		this.lastConnectorPool = connectorPool;
		this.lastNode = node;
	}
}
