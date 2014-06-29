package org.nem.nis;

import net.minidev.json.*;
import org.nem.core.async.*;
import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.deploy.CommonStarter;
import org.nem.nis.audit.AuditCollection;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.*;
import org.nem.peer.services.PeerNetworkServicesFactory;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.NodeExperiences;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements AutoCloseable {

	//region constants

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

	private static final int UPDATE_LOCAL_NODE_ENDPOINT_DELAY = 5 * ONE_MINUTE;

	private static final int MAX_AUDIT_HISTORY_SIZE = 50;

	//endregion

	private final AccountLookup accountLookup;
	private final BlockChain blockChain;
	private final CountingBlockSynchronizer synchronizer;
	private AsyncTimer foragingTimer;
	private PeerNetworkHost host;
	private final AtomicBoolean isBootAttempted = new AtomicBoolean(false);
	private final List<NisAsyncTimerVisitor> timerVisitors = new ArrayList<>();
	private final AuditCollection outgoingAudits = createAuditCollection();
	private final AuditCollection incomingAudits = createAuditCollection();

	@Autowired(required = true)
	public NisPeerNetworkHost(final AccountLookup accountLookup, final BlockChain blockChain) {
		this.accountLookup = accountLookup;
		this.blockChain = blockChain;
		this.synchronizer = new CountingBlockSynchronizer(this.blockChain);
	}

	/**
	 * Boots the network.
	 *
	 * @param localNode The local node.
	 */
	public CompletableFuture boot(final Node localNode) {
		final Config config = new Config(
				localNode,
				loadJsonObject("peers-config.json"),
				CommonStarter.META_DATA.getVersion());

		return this.boot(config);
	}

	/**
	 * Boots the network.
	 *
	 * @param config The network configuration.
	 */
	public CompletableFuture boot(final Config config) {
		if (!this.isBootAttempted.compareAndSet(false, true))
			throw new IllegalStateException("network boot was already attempted");

		final PeerNetworkState networkState = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());
		final NisNodeSelectorFactory selectorFactory = new NisNodeSelectorFactory(config, networkState);
		final PeerNetwork network = new PeerNetwork(
				networkState,
				createNetworkServicesFactory(networkState),
				selectorFactory);
		return network.updateLocalNodeEndpoint()
				.handle((v, e) -> {
					if (null != e) {
						this.isBootAttempted.set(false);
						throw new IllegalStateException("network boot failed", e);
					}

					this.host = new PeerNetworkHost(network);
					this.timerVisitors.addAll(this.host.getVisitors());

					final NisAsyncTimerVisitor foragingTimerVisitor = PeerNetworkHost.createNamedVisitor("FORAGING");
					this.timerVisitors.add(foragingTimerVisitor);

					this.foragingTimer = new AsyncTimer(
							this.host.runnableToFutureSupplier(() -> {
								final Block block = this.blockChain.forageBlock();
								if (null == block)
									return;

								final SecureSerializableEntity<?> secureBlock = new SecureSerializableEntity<>(
										block,
										this.host.getNetwork().getLocalNode().getIdentity());
								this.getNetwork().broadcast(NodeApiId.REST_PUSH_BLOCK, secureBlock);
							}),
							FORAGING_INITIAL_DELAY,
							new UniformDelayStrategy(FORAGING_INTERVAL),
							foragingTimerVisitor);
					return null;
				});
	}

	private static JSONObject loadJsonObject(final String configFileName) {
		try {
			try (final InputStream fin = NisPeerNetworkHost.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin)
					throw new FatalConfigException(String.format("Configuration file <%s> not available", configFileName));

				return (JSONObject)JSONValue.parse(fin);
			}
		} catch (Exception e) {
			throw new FatalConfigException("Exception encountered while loading config", e);
		}
	}

	/**
	 * Gets the hosted network.
	 *
	 * @return The hosted network.
	 */
	public PeerNetwork getNetwork() {
		if (null == this.host)
			throw new IllegalStateException("network has not been booted yet");

		return this.host.getNetwork();
	}

	/**
	 * Gets outgoing audit information.
	 *
	 * @return The outgoing audit information.
	 */
	public AuditCollection getOutgoingAudits() {
		return this.outgoingAudits;
	}

	/**
	 * Gets incoming audit information.
	 *
	 * @return The incoming audit information.
	 */
	public AuditCollection getIncomingAudits() {
		return this.incomingAudits;
	}

	/**
	 * Gets the number of sync attempts with the specified node.
	 *
	 * @param node The node to sync with.
	 * @return The number of sync attempts with the specified node.
	 */
	public int getSyncAttempts(final Node node) {
		return this.synchronizer.getSyncAttempts(node);
	}

	/**
	 * Gets all timer visitors.
	 *
	 * @return All timer visitors.
	 */
	public List<NisAsyncTimerVisitor> getVisitors() {
		final List<NisAsyncTimerVisitor> visitors = new ArrayList<>();
		visitors.addAll(this.timerVisitors);
		return visitors;
	}

	@Override
	public void close() {
		if (null != foragingTimer)
			this.foragingTimer.close();

		if (null != this.host)
			this.host.close();
	}

	private PeerNetworkServicesFactory createNetworkServicesFactory(final PeerNetworkState networkState) {
		final HttpConnectorPool connectorPool = new HttpConnectorPool(this.getOutgoingAudits());
		final PeerConnector connector = connectorPool.getPeerConnector(this.accountLookup);
		return new PeerNetworkServicesFactory(networkState, connector, connectorPool, this.synchronizer);
	}

	private static AuditCollection createAuditCollection() {
		return new AuditCollection(MAX_AUDIT_HISTORY_SIZE, CommonStarter.TIME_PROVIDER);
	}

	private static class PeerNetworkHost implements AutoCloseable {
		private final PeerNetwork network;
		private final AsyncTimer refreshTimer;
		private final List<NisAsyncTimerVisitor> timerVisitors;
		private final List<AsyncTimer> secondaryTimers;
		private final Executor executor = Executors.newCachedThreadPool();

		public PeerNetworkHost(final PeerNetwork network) {
			this.network = network;
			this.timerVisitors = new ArrayList<>();

			this.timerVisitors.add(PeerNetworkHost.createNamedVisitor("REFRESH"));
			this.refreshTimer = new AsyncTimer(
					() -> this.network.refresh(),
					REFRESH_INITIAL_DELAY,
					getRefreshDelayStrategy(),
					this.timerVisitors.get(0));

			this.secondaryTimers = Arrays.asList(
					this.createSecondaryAsyncTimer(
							() -> this.network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences()),
							BROADCAST_INTERVAL,
							"BROADCAST"),
					this.createSecondaryAsyncTimer(
							this.runnableToFutureSupplier(() -> this.network.synchronize()),
							SYNC_INTERVAL,
							"SYNC"),
					this.createSecondaryAsyncTimer(
							this.runnableToFutureSupplier(() -> this.network.pruneInactiveNodes()),
							PRUNE_INACTIVE_NODES_DELAY,
							"PRUNING INACTIVE NODES"),
					this.createSecondaryAsyncTimer(
							this.runnableToFutureSupplier(() -> this.network.updateLocalNodeEndpoint()),
							UPDATE_LOCAL_NODE_ENDPOINT_DELAY,
							"UPDATING LOCAL NODE ENDPOINT"));
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
			final NisAsyncTimerVisitor visitor = this.createNamedVisitor(name);
			this.timerVisitors.add(visitor);

			return AsyncTimer.After(
					this.refreshTimer,
					recurringFutureSupplier,
					new UniformDelayStrategy(delay),
					visitor);
		}

		public PeerNetwork getNetwork() {
			return this.network;
		}

		public List<NisAsyncTimerVisitor> getVisitors() {
			return this.timerVisitors;
		}

		@Override
		public void close() {
			this.refreshTimer.close();
			this.secondaryTimers.forEach(obj -> obj.close());
		}

		public Supplier<CompletableFuture<?>> runnableToFutureSupplier(final Runnable runnable) {
			return () -> CompletableFuture.runAsync(runnable, this.executor);
		}

		public static NisAsyncTimerVisitor createNamedVisitor(final String name) {
			return new NisAsyncTimerVisitor(name, CommonStarter.TIME_PROVIDER);
		}
	}

	private static class NisNodeSelectorFactory implements NodeSelectorFactory {
		private final Config config;
		private final PeerNetworkState state;

		public NisNodeSelectorFactory(final Config config, final PeerNetworkState state) {
			this.config = config;
			this.state = state;
		}

		@Override
		public NodeSelector createNodeSelector() {
			final TrustContext context = this.state.getTrustContext();
			final SecureRandom random = new SecureRandom();
			return new PreTrustAwareNodeSelector(
					new BasicNodeSelector(
							10, // TODO: read from configuration
							new ActiveNodeTrustProvider(this.config.getTrustProvider(), this.state.getNodes()),
							context,
							random),
					this.state.getNodes(),
					context,
					random);
		}
	}
}
