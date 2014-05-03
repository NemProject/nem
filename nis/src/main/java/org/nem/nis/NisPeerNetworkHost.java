package org.nem.nis;

import org.nem.core.async.*;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.NodeApiId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements Runnable, AutoCloseable {

	private static final int REFRESH_INITIAL_DELAY = 200;
	private static final int REFRESH_INTERVAL = 1 * 60 * 1000;
	private static final int SYNC_INTERVAL = 1000;
	private static final int BROADCAST_INTERVAL = 5 * 60 * 1000;

	private final AccountLookup accountLookup;
	private final BlockChain blockChain;
	private PeerNetworkHost host;
	private final ScheduledThreadPoolExecutor blockGeneratorExecutor;

	@Autowired(required = true)
	public NisPeerNetworkHost(final AccountLookup accountLookup, final BlockChain blockChain) {
		this.accountLookup = accountLookup;
		this.blockChain = blockChain;
		this.blockGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
	}

	/**
	 * Boots the network.
	 */
	public void boot() {
		final PeerNetwork network = new PeerNetwork(
				Config.fromFile("peers-config.json"),
				createNetworkServices());
		this.host = new PeerNetworkHost(network);
	}

	public void bootForaging() {
		this.blockGeneratorExecutor.scheduleWithFixedDelay(this, 5, 3, TimeUnit.SECONDS);
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
	public void run() {
		Block block = blockChain.forageBlock();
		if (block != null) {
			this.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, block);
		}
	}

	@Override
	public void close() {
		this.blockGeneratorExecutor.shutdownNow();

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
					getRefreshDelayStrategy());
			this.refreshTimer.setName("REFRESH");

			this.broadcastTimer = AsyncTimer.After(
					this.refreshTimer,
					() -> this.network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences()),
					new UniformDelayStrategy(BROADCAST_INTERVAL));
			this.broadcastTimer.setName("BROADCAST");

			this.syncTimer = AsyncTimer.After(
					this.refreshTimer,
					() -> CompletableFuture.runAsync(this.network::synchronize),
					new UniformDelayStrategy(SYNC_INTERVAL));
			this.syncTimer.setName("SYNC");
		}

		private static AbstractDelayStrategy getRefreshDelayStrategy() {
			// initially refresh at 1/6 of the desired rate, gradually increase to the desired rate
			// over 60 iterations, and then plateau at that rate forever
			final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
					new LinearDelayStrategy(REFRESH_INTERVAL / 6, REFRESH_INTERVAL, 60),
					new UniformDelayStrategy(REFRESH_INTERVAL));
			return new AggregateDelayStrategy(subStrategies);
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
