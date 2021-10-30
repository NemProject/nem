package org.nem.peer.services;

import org.nem.core.node.Node;
import org.nem.peer.*;
import org.nem.peer.connect.SyncConnectorPool;
import org.nem.peer.trust.NodeSelector;

import java.util.logging.Logger;

/**
 * Helper class used to implement network synchronization logic.
 */
public class NodeSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(NodeSynchronizer.class.getName());

	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;
	private final PeerNetworkState state;

	/**
	 * Creates a new synchronizer.
	 *
	 * @param syncConnectorPool The sync connector pool to use.
	 * @param blockSynchronizer The block synchronizer to use.
	 * @param state The network state.
	 */
	public NodeSynchronizer(final SyncConnectorPool syncConnectorPool, final BlockSynchronizer blockSynchronizer,
			final PeerNetworkState state) {
		this.syncConnectorPool = syncConnectorPool;
		this.blockSynchronizer = blockSynchronizer;
		this.state = state;
	}

	/**
	 * Synchronizes this node with a remote node.
	 *
	 * @param selector The node selector.
	 * @return The future.
	 */
	public boolean synchronize(final NodeSelector selector) {
		final Node partnerNode = selector.selectNode();
		if (null == partnerNode) {
			LOGGER.warning("no suitable peers found to sync with");
			return false;
		}

		LOGGER.info(String.format("synchronizing with %s", partnerNode));
		final NodeInteractionResult result = this.blockSynchronizer.synchronizeNode(this.syncConnectorPool, partnerNode);
		LOGGER.info(String.format("synchronizing with %s finished", partnerNode));
		this.state.updateExperience(partnerNode, result);
		return true;
	}
}
