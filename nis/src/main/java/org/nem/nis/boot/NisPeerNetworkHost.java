package org.nem.nis.boot;

import net.minidev.json.*;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.model.NetworkInfos;
import org.nem.core.node.*;
import org.nem.deploy.CommonStarter;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.connect.*;
import org.nem.nis.time.synchronization.*;
import org.nem.nis.time.synchronization.filter.*;
import org.nem.peer.*;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.node.NodeCompatibilityChecker;
import org.nem.peer.services.*;
import org.nem.peer.trust.TrustProvider;
import org.nem.peer.trust.score.NodeExperiences;
import org.nem.specific.deploy.*;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The NIS peer network host.
 */
public class NisPeerNetworkHost implements AutoCloseable {
	private final ReadOnlyNisCache nisCache;
	private final CountingBlockSynchronizer synchronizer;
	private final PeerNetworkScheduler scheduler;
	private final ChainServices chainServices;
	private final NodeCompatibilityChecker compatibilityChecker;
	private final NisConfiguration nisConfiguration;
	private final HttpConnectorPool httpConnectorPool;
	private final TrustProvider trustProvider;
	private final AuditCollection incomingAudits;
	private final AuditCollection outgoingAudits;
	private final AtomicReference<PeerNetworkBootstrapper> peerNetworkBootstrapper = new AtomicReference<>();
	private PeerNetwork network;
	private PeerNetworkBroadcastBuffer networkBroadcastBuffer;

	/**
	 * Creates a NIS peer network host.
	 *
	 * @param nisCache The nis cache.
	 * @param synchronizer The block synchronizer.
	 * @param scheduler The network scheduler.
	 * @param chainServices The remote block chain service.
	 * @param compatibilityChecker The node compatibility checker.
	 * @param nisConfiguration The nis configuration.
	 * @param httpConnectorPool The factory of http connectors.
	 * @param trustProvider The trust provider.
	 * @param incomingAudits The incoming audits
	 * @param outgoingAudits The outgoing audits.
	 */
	public NisPeerNetworkHost(final ReadOnlyNisCache nisCache, final CountingBlockSynchronizer synchronizer,
			final PeerNetworkScheduler scheduler, final ChainServices chainServices, final NodeCompatibilityChecker compatibilityChecker,
			final NisConfiguration nisConfiguration, final HttpConnectorPool httpConnectorPool, final TrustProvider trustProvider,
			final AuditCollection incomingAudits, final AuditCollection outgoingAudits) {
		this.nisCache = nisCache;
		this.synchronizer = synchronizer;
		this.scheduler = scheduler;
		this.chainServices = chainServices;
		this.compatibilityChecker = compatibilityChecker;
		this.nisConfiguration = nisConfiguration;
		this.httpConnectorPool = httpConnectorPool;
		this.trustProvider = trustProvider;
		this.incomingAudits = incomingAudits;
		this.outgoingAudits = outgoingAudits;
	}

	/**
	 * Boots the network. Note that this is scoped to the boot package to prevent it from being called externally (boot should be called on
	 * the injected NetworkHostBootstrapper).
	 *
	 * @param localNode The local node.
	 * @return Void future.
	 */
	CompletableFuture<Void> boot(final Node localNode) {
		final Config config = new Config(localNode,
				loadJsonObject(String.format("peers-config_%s.json", this.nisConfiguration.getNetworkName())),
				CommonStarter.META_DATA.getVersion(), NetworkInfos.getDefault().getVersion(), this.nisConfiguration.getOptionalFeatures());

		this.peerNetworkBootstrapper.compareAndSet(null, this.createPeerNetworkBootstrapper(config));
		return this.peerNetworkBootstrapper.get().boot().thenAccept(network -> {
			this.networkBroadcastBuffer = new PeerNetworkBroadcastBuffer(network, new BroadcastBuffer());
			this.network = network;
			this.scheduler.addTasks(this.network, this.networkBroadcastBuffer, this.nisConfiguration.useNetworkTime(),
					IpDetectionMode.Disabled != this.nisConfiguration.getIpDetectionMode());
		});
	}

	private static JSONObject loadJsonObject(final String configFileName) {
		try {
			try (final InputStream fin = NisPeerNetworkHost.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin) {
					throw new FatalConfigException(String.format("Configuration file <%s> not available", configFileName));
				}

				return (JSONObject) JSONValue.parse(fin);
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
		this.checkNetwork();
		return this.network;
	}

	/**
	 * Gets the hosted network broadcast buffer.
	 *
	 * @return The hosted network broadcast buffer.
	 */
	public PeerNetworkBroadcastBuffer getNetworkBroadcastBuffer() {
		this.checkNetwork();
		return this.networkBroadcastBuffer;
	}

	private void checkNetwork() {
		if (null == this.network) {
			throw new NisIllegalStateException(NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_NOT_BOOTED);
		}
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

	public CompletableFuture<Node> getNodeInfo(final Node node) {
		final PeerConnector peerConnector = this.httpConnectorPool.getPeerConnector(this.nisCache.getAccountCache());
		return peerConnector.getInfo(node);
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
				new AggregateSynchronizationFilter(
						Arrays.asList(new ResponseDelayDetectionFilter(), new ClampingFilter(), new AlphaTrimmedMeanFilter())),
				this.nisCache.getPoxFacade(), this.nisCache.getAccountStateCache());
	}

	private PeerNetworkServicesFactory createNetworkServicesFactory(final PeerNetworkState networkState) {
		final PeerConnector peerConnector = this.httpConnectorPool.getPeerConnector(this.nisCache.getAccountCache());
		final TimeSynchronizationConnector timeSynchronizationConnector = this.httpConnectorPool
				.getTimeSyncConnector(this.nisCache.getAccountCache());
		return new DefaultPeerNetworkServicesFactory(networkState, peerConnector, timeSynchronizationConnector, this.httpConnectorPool,
				this.synchronizer, this.chainServices, this.createTimeSynchronizationStrategy(), this.compatibilityChecker);
	}

	private PeerNetworkBootstrapper createPeerNetworkBootstrapper(final Config config) {
		final PeerNetworkState networkState = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());
		final PeerNetworkNodeSelectorFactory selectorFactory = new DefaultPeerNetworkNodeSelectorFactory(this.nisConfiguration,
				this.trustProvider, networkState, this.nisCache.getPoxFacade(), this.nisCache.getAccountStateCache());
		return new PeerNetworkBootstrapper(networkState, this.createNetworkServicesFactory(networkState), selectorFactory,
				this.nisConfiguration.getIpDetectionMode());
	}
}
