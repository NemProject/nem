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
import java.util.function.Supplier;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements AutoCloseable {
	private static final int ONE_SECOND = 1000;
	private static final int ONE_MINUTE = 60 * ONE_SECOND;
	private static final int ONE_HOUR = 60 * ONE_MINUTE;

	private static final int REFRESH_INITIAL_DELAY = 200;
	private static final int REFRESH_INITIAL_INTERVAL = ONE_SECOND;
	private static final int REFRESH_PLATEAU_INTERVAL = 5 * ONE_MINUTE;
	private static final int REFRESH_BACK_OFF_TIME = 12 * ONE_HOUR;

	private static final int SYNC_INTERVAL = 5 * ONE_SECOND;

	private static final int BROADCAST_INTERVAL = 5 * ONE_MINUTE;

	private static final int FORAGING_INITIAL_DELAY = 5 * ONE_SECOND;
	private static final int FORAGING_INTERVAL = 3 * ONE_SECOND;

	private static final int PRUNE_INACTIVE_NODES_DELAY = ONE_HOUR;

	private final AccountLookup accountLookup;
	private final BlockChain blockChain;
	private AsyncTimer foragingTimer;
	private PeerNetworkHost host;

	@Autowired(required = true)
	public NisPeerNetworkHost(final AccountLookup accountLookup, final BlockChain blockChain) {
		this.accountLookup = accountLookup;
		this.blockChain = blockChain;
	}

	/**
	 * Boots the network.
	 */
	public CompletableFuture boot() {
		return PeerNetwork.createWithVerificationOfLocalNode(Config.fromFile("peers-config.json"), createNetworkServices())
				.thenAccept(network -> {
					this.host = new PeerNetworkHost(network);

					this.foragingTimer = new AsyncTimer(
							() -> CompletableFuture.runAsync(() -> {
								final Block block = this.blockChain.forageBlock();
								if (null != block)
									this.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, block);
							}),
							FORAGING_INITIAL_DELAY,
							new UniformDelayStrategy(FORAGING_INTERVAL));
					this.foragingTimer.setName("FORAGING");
				});
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
		this.foragingTimer.close();
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
		private final List<AsyncTimer> secondaryTimers;

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

			this.secondaryTimers = Arrays.asList(
					this.createSecondaryAsyncTimer(
							() -> this.network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences()),
							BROADCAST_INTERVAL,
							"BROADCAST"),
					this.createSecondaryAsyncTimer(
							() -> CompletableFuture.runAsync(this.network::synchronize),
							SYNC_INTERVAL,
							"SYNC"),
					this.createSecondaryAsyncTimer(
							() -> CompletableFuture.runAsync(this.network::pruneInactiveNodes),
							PRUNE_INACTIVE_NODES_DELAY,
							"PRUNING INACTIVE NODES"));
		}

		private static AbstractDelayStrategy getRefreshDelayStrategy() {
			// initially refresh at REFRESH_INITIAL_INTERVAL (1s), gradually increasing to
			// REFRESH_PLATEAU_INTERVAL (5m) over REFRESH_BACK_OFF_TIME (12 hours),
			// and then plateau at that rate forever
			final List<AbstractDelayStrategy> subStrategies = Arrays.asList(
					LinearDelayStrategy.withDuration(
							REFRESH_INITIAL_INTERVAL,
							REFRESH_PLATEAU_INTERVAL,
							REFRESH_BACK_OFF_TIME),
					new UniformDelayStrategy(REFRESH_PLATEAU_INTERVAL));
			return new AggregateDelayStrategy(subStrategies);
		}

		private AsyncTimer createSecondaryAsyncTimer(
				final Supplier<CompletableFuture<?>> recurringFutureSupplier,
				final int delay,
				final String name) {
			final AsyncTimer timer = AsyncTimer.After(
					this.refreshTimer,
					recurringFutureSupplier,
					new UniformDelayStrategy(delay));
			timer.setName(name);
			return timer;
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
			this.secondaryTimers.forEach(AsyncTimer::close);
		}
	}
}
