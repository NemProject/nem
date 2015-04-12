package org.nem.peer.test;

import org.nem.core.node.Node;
import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;

/**
 * A mock BlockSynchronizer implementation.
 */
public class MockBlockSynchronizer implements BlockSynchronizer {

	private int numSynchronizeNodeCalls;
	private SyncConnectorPool lastConnectorPool;
	private Node lastNode;
	private NodeInteractionResult synchronizeNodeResult;

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

	/**
	 * Sets the result that should be returned from synchronizeNode.
	 *
	 * @param result The result.
	 */
	public void setSynchronizeNodeResult(final NodeInteractionResult result) {
		this.synchronizeNodeResult = result;
	}

	@Override
	public NodeInteractionResult synchronizeNode(final SyncConnectorPool connectorPool, final Node node) {
		++this.numSynchronizeNodeCalls;
		this.lastConnectorPool = connectorPool;
		this.lastNode = node;
		return this.synchronizeNodeResult;
	}
}
