package org.nem.nis;

import net.minidev.json.*;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.node.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.*;
import org.nem.nis.service.ChainServices;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.services.PeerNetworkServicesFactory;
import org.nem.peer.trust.score.NodeExperiences;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements AutoCloseable {
	private static final int MAX_AUDIT_HISTORY_SIZE = 50;

	private final AccountLookup accountLookup;
	private final BlockChain blockChain;
	// TODO 20140909 J-B - i think you should inject this
	// TODO 20140910 BR -> J done. I first had it injected but then thought since we already have the block chain object it is
	// TODO                  not necessary to do it (I have to add another bean to NisAppConfig too). Why is injecting better?
	private final ChainServices chainServices;
	private final NisConfiguration nisConfiguration;
	private final CountingBlockSynchronizer synchronizer;
	private final AuditCollection outgoingAudits = createAuditCollection();
	private final AuditCollection incomingAudits = createAuditCollection();
	private final AtomicReference<PeerNetworkBootstrapper> peerNetworkBootstrapper = new AtomicReference<>();
	private final PeerNetworkScheduler scheduler = new PeerNetworkScheduler(CommonStarter.TIME_PROVIDER);
	private PeerNetwork network;

	@Autowired(required = true)
	public NisPeerNetworkHost(
			final AccountLookup accountLookup,
			final BlockChain blockChain,
			final ChainServices chainServices,
			final NisConfiguration nisConfiguration) {
		this.accountLookup = accountLookup;
		this.blockChain = blockChain;
		this.chainServices = chainServices;
		this.nisConfiguration = nisConfiguration;
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

		this.peerNetworkBootstrapper.compareAndSet(null, this.createPeerNetworkBootstrapper(config));
		return this.peerNetworkBootstrapper.get().boot().thenAccept(network -> {
			this.network = network;
			this.scheduler.addTasks(this.network, this.blockChain);
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
	 * Gets a value indicating whether or not the network is already booted.
	 *
	 * TODO 20140909 J-B: can you check this in NisPeerNetworkHostTest (an integration test)
	 * TODO 20140910 BR -> J done.
	 *
	 * @return true if the network is booted, false otherwise.
	 */
	public boolean isNetworkBooted() {
		return (null != this.network);
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
		return this.scheduler.getVisitors();
	}

	@Override
	public void close() {
		this.scheduler.close();
	}

	private PeerNetworkServicesFactory createNetworkServicesFactory(final PeerNetworkState networkState) {
		final CommunicationMode communicationMode = this.nisConfiguration.useBinaryTransport()
				? CommunicationMode.BINARY
				: CommunicationMode.JSON;
		final HttpConnectorPool connectorPool = new HttpConnectorPool(communicationMode, this.getOutgoingAudits());
		final PeerConnector connector = connectorPool.getPeerConnector(this.accountLookup);
		return new PeerNetworkServicesFactory(networkState, connector, connectorPool, this.synchronizer, this.chainServices);
	}

	private PeerNetworkBootstrapper createPeerNetworkBootstrapper(final Config config) {
		final PeerNetworkState networkState = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());
		final NisNodeSelectorFactory selectorFactory = new NisNodeSelectorFactory(
				this.nisConfiguration.getNodeLimit(),
				config.getTrustProvider(),
				networkState);
		return new PeerNetworkBootstrapper(
				networkState,
				this.createNetworkServicesFactory(networkState),
				selectorFactory,
				!this.nisConfiguration.bootWithoutAck());
	}

	private static AuditCollection createAuditCollection() {
		return new AuditCollection(MAX_AUDIT_HISTORY_SIZE, CommonStarter.TIME_PROVIDER);
	}
}
