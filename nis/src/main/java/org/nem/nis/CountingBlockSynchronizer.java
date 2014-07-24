package org.nem.nis;

import org.nem.core.node.Node;
import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;

import java.util.concurrent.*;

/**
 * Decorator that counts the number of times each remote node was interacted with.
 */
public class CountingBlockSynchronizer implements BlockSynchronizer {
	private final BlockSynchronizer synchronizer;
	private final ConcurrentMap<Node, Integer> nodeToCountMap;

	/**
	 * Creates a new counting block synchronizer.
	 *
	 * @param synchronizer The wrapped synchronizer.
	 */
	public CountingBlockSynchronizer(final BlockSynchronizer synchronizer) {
		this.synchronizer = synchronizer;
		this.nodeToCountMap = new ConcurrentHashMap<>();
	}

	/**
	 * Gets the number of sync attempts with the specified node.
	 *
	 * @param node The node to sync with.
	 * @return The number of sync attempts with the specified node.
	 */
	public int getSyncAttempts(final Node node) {
		return this.nodeToCountMap.getOrDefault(node, 0);
	}

	@Override
	public NodeInteractionResult synchronizeNode(final SyncConnectorPool connector, final Node node) {
		this.nodeToCountMap.put(node, this.getSyncAttempts(node) + 1);
		return this.synchronizer.synchronizeNode(connector, node);
	}
}
