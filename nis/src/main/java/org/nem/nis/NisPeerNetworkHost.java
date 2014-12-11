package org.nem.nis;

import net.minidev.json.*;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.node.*;
import org.nem.deploy.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.*;
import org.nem.nis.cache.NisCache;
import org.nem.nis.service.ChainServices;
import org.nem.nis.time.synchronization.*;
import org.nem.nis.time.synchronization.filter.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.services.PeerNetworkServicesFactory;
import org.nem.peer.trust.score.NodeExperiences;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements AutoCloseable {
	private final NisCache nisCache;
	private final CountingBlockSynchronizer synchronizer;
	private final PeerNetworkScheduler scheduler;
	private final ChainServices chainServices;
	private final NisConfiguration nisConfiguration;
	private final HttpConnectorPool httpConnectorPool;
	private final AuditCollection incomingAudits;
	private final AuditCollection outgoingAudits;
	private final AtomicReference<PeerNetworkBootstrapper> peerNetworkBootstrapper = new AtomicReference<>();
	private PeerNetwork network;

	@Autowired(required = true)
	public NisPeerNetworkHost(
			final NisCache nisCache,
			final CountingBlockSynchronizer synchronizer,
			final PeerNetworkScheduler scheduler,
			final ChainServices chainServices,
			final NisConfiguration nisConfiguration,
			final HttpConnectorPool httpConnectorPool,
			final AuditCollection incomingAudits,
			final AuditCollection outgoingAudits) {
		this.nisCache = nisCache;
		this.synchronizer = synchronizer;
		this.scheduler = scheduler;
		this.chainServices = chainServices;
		this.nisConfiguration = nisConfiguration;
		this.httpConnectorPool = httpConnectorPool;
		this.incomingAudits = incomingAudits;
		this.outgoingAudits = outgoingAudits;
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

		this.peerNetworkBootstrapper.compareAndSet(null, this.createPeerNetworkBootstrapper(config));
		return this.peerNetworkBootstrapper.get().boot().thenAccept(network -> {
			this.network = network;
			this.scheduler.addTasks(
					this.network,
					this.nisConfiguration.useNetworkTime(),
					IpDetectionMode.Disabled != this.nisConfiguration.getIpDetectionMode());
		});
	}

	private static JSONObject loadJsonObject(final String configFileName) {
		try {
			try (final InputStream fin = NisPeerNetworkHost.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin) {
					throw new FatalConfigException(String.format("Configuration file <%s> not available", configFileName));
				}

				return (JSONObject)JSONValue.parse(fin);
			}
		} catch (final Exception e) {
			throw new FatalConfigException("Exception encountered while loading config", e);
		}
	}

	/**
	 * Gets the hosted network.
	 *
	 * @return The hosted network.
	 */
	public PeerNetwork getNetwork() {
		if (null == this.network) {
			throw new IllegalStateException("network has not been booted yet");
		}

		return this.network;
	}

	/**
	 * Gets a value indicating whether or not the network is currently being booted.
	 *
	 * @return true if the network is being booted, false otherwise.
	 */
	public boolean isNetworkBooting() {
		return !this.isNetworkBooted() && null != this.peerNetworkBootstrapper.get();
	}

	/**
	 * Gets a value indicating whether or not the network is already booted.
	 *
	 * @return true if the network is booted, false otherwise.
	 */
	public boolean isNetworkBooted() {
		return null != this.network;
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
	public List<NemAsyncTimerVisitor> getVisitors() {
		return this.scheduler.getVisitors();
	}

	@Override
	public void close() {
		this.scheduler.close();
	}

	private TimeSynchronizationStrategy createTimeSynchronizationStrategy() {
		return new DefaultTimeSynchronizationStrategy(
				new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter())),
				this.nisCache.getPoiFacade());
	}

	private PeerNetworkServicesFactory createNetworkServicesFactory(final PeerNetworkState networkState) {
		final PeerConnector peerConnector = this.httpConnectorPool.getPeerConnector(this.nisCache.getAccountCache());
		final TimeSynchronizationConnector timeSynchronizationConnector = this.httpConnectorPool.getTimeSyncConnector(this.nisCache.getAccountCache());
		return new PeerNetworkServicesFactory(
				networkState,
				peerConnector,
				timeSynchronizationConnector,
				this.httpConnectorPool,
				this.synchronizer,
				this.chainServices,
				this.createTimeSynchronizationStrategy());
	}

	private PeerNetworkBootstrapper createPeerNetworkBootstrapper(final Config config) {
		final PeerNetworkState networkState = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());
		final NisNodeSelectorFactory selectorFactory = new NisNodeSelectorFactory(
				this.nisConfiguration.getNodeLimit(),
				config.getTrustProvider(),
				networkState);
		final ImportanceAwareNodeSelectorFactory importanceAwareSelectorFactory = new ImportanceAwareNodeSelectorFactory(
				this.nisConfiguration.getTimeSyncNodeLimit(),
				config.getTrustProvider(),
				networkState,
				this.nisCache.getPoiFacade());
		return new PeerNetworkBootstrapper(
				networkState,
				this.createNetworkServicesFactory(networkState),
				selectorFactory,
				importanceAwareSelectorFactory,
				this.nisConfiguration.getIpDetectionMode());
	}
}
