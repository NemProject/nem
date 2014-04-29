package org.nem.nis;

import org.nem.core.serialization.AccountLookup;
import org.nem.core.utils.AsyncTimer;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.NodeApiId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements AutoCloseable {

	private static final int REFRESH_INITIAL_DELAY = 200;
	private static final int REFRESH_INTERVAL = 1 * 60 * 1000;
	private static final int SYNC_INTERVAL = 1000;
	private static final int BROADCAST_INTERVAL = 5 * 60 * 1000;

	@Autowired
	private AccountLookup accountLookup;

	@Autowired
	private BlockChain blockChain;

	private PeerNetworkHost host;

	/**
	 * Boots the network.
	 */
	public void boot() {
		final PeerNetwork network = new PeerNetwork(
				Config.fromFile("peers-config.json"),
				createNetworkServices());
		this.host = new PeerNetworkHost(network);
	}

	/**
	 * Gets the hosted network.
	 *
	 * @return The hosted network.
	 */
	public PeerNetwork getNetwork() {
		return this.host.getNetwork();
	}

	@Override
	public void close() {
		this.host.close();
	}

	private PeerNetworkServices createNetworkServices() {
		final HttpConnectorPool connectorPool = new HttpConnectorPool();
		final PeerConnector connector = connectorPool.getPeerConnector(this.accountLookup);
		return new PeerNetworkServices(connector, connectorPool, this.blockChain);
	}

	private static class PeerNetworkHost implements AutoCloseable {

		private final PeerNetwork network;
		private final AsyncTimer refreshTimer;
		private final AsyncTimer broadcastTimer;
		private final AsyncTimer syncTimer;

		/**
		 * Creates a host that hosts the specified network.
		 *
		 * @param network The network.
		 */
		public PeerNetworkHost(final PeerNetwork network) {
			this.network = network;

			this.refreshTimer = new AsyncTimer(
					this.network::refresh,
					REFRESH_INITIAL_DELAY,
					REFRESH_INTERVAL);
			this.refreshTimer.setName("REFRESH");

			this.broadcastTimer = AsyncTimer.After(
					this.refreshTimer,
					() -> this.network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences()),
					BROADCAST_INTERVAL);
			this.broadcastTimer.setName("BROADCAST");

			this.syncTimer = AsyncTimer.After(
					this.refreshTimer,
					() -> CompletableFuture.runAsync(this.network::synchronize),
					SYNC_INTERVAL);
			this.syncTimer.setName("SYNC");
		}

		/**
		 * Gets the hosted network.
		 *
		 * @return The hosted network.
		 */
		public PeerNetwork getNetwork() {
			return this.network;
		}

		@Override
		public void close() {
			this.refreshTimer.close();
			this.broadcastTimer.close();
			this.syncTimer.close();
		}
	}
}
