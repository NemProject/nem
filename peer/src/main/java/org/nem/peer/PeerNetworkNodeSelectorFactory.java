package org.nem.peer;

import org.nem.peer.trust.NodeSelector;

/**
 * Interface for selected nodes used by the peer network.
 */
public interface PeerNetworkNodeSelectorFactory {

	/**
	 * Creates a node selector for refresh operations.
	 *
	 * @return The node selector.
	 */
	NodeSelector createRefreshNodeSelector();

	/**
	 * Creates a node selector for update operations.
	 *
	 * @return The node selector.
	 */
	NodeSelector createUpdateNodeSelector();

	/**
	 * Creates a node selector for time sync operations.
	 *
	 * @return The node selector.
	 */
	NodeSelector createTimeSyncNodeSelector();
}
