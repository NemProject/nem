package org.nem.peer;

import org.nem.core.connect.*;

/**
 * Aggregate of PeerNetwork dependencies.
 */
public class PeerNetworkServices {

	private final PeerConnector peerConnector;
	private final SyncConnectorPool syncConnectorPool;
	private final BlockSynchronizer blockSynchronizer;

	/**
	 * Creates a new services aggregate.
	 *
	 * @param peerConnector     The peer connector to use.
	 * @param syncConnectorPool     The sync connector pool to use.
	 * @param blockSynchronizer The block synchronizer to use.
	 */
	public PeerNetworkServices(
			final PeerConnector peerConnector,
			final SyncConnectorPool syncConnectorPool,
			final BlockSynchronizer blockSynchronizer) {
		this.peerConnector = peerConnector;
		this.syncConnectorPool = syncConnectorPool;
		this.blockSynchronizer = blockSynchronizer;
	}

	/**
	 * Gets the peer connector.
	 *
	 * @return The peer connector.
	 */
	public PeerConnector getPeerConnector() { return this.peerConnector; }

	/**
	 * Gets the sync connector pool.
	 *
	 * @return The sync connector pool.
	 */
	public SyncConnectorPool getSyncConnectorPool() { return this.syncConnectorPool; }

	/**
	 * Gets the block synchronizer.
	 *
	 * @return The block synchronizer.
	 */
	public BlockSynchronizer getBlockSynchronizer() { return this.blockSynchronizer; }
}
