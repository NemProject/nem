package org.nem.nis;

import org.nem.core.serialization.DeserializationContext;
import org.nem.peer.*;
import org.nem.peer.net.HttpPeerConnector;
import org.nem.peer.scheduling.ParallelSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost {

	private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
	private static final int REFRESH_INITIAL_DELAY = 200;
	private static final int REFRESH_INTERVAL = 1000;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	private PeerNetworkHost host;

	/**
	 * Boots the network.
	 */
	public void boot() {
		this.host = new PeerNetworkHost(
				new PeerNetwork(Config.fromFile("peers-config.json"), createNetworkServices()),
				REFRESH_INITIAL_DELAY,
				REFRESH_INTERVAL);
	}

	/**
	 * Gets the hosted network.
	 *
	 * @return The hosted network.
	 */
	public PeerNetwork getNetwork() {
		return this.host.getNetwork();
	}

	private PeerNetworkServices createNetworkServices() {
		final HttpPeerConnector connector = new HttpPeerConnector(new DeserializationContext(this.accountAnalyzer));
		final ParallelSchedulerFactory<Node> schedulerFactory = new ParallelSchedulerFactory<>(2 * NUM_CORES);
		return new PeerNetworkServices(connector, connector, schedulerFactory, this.blockChain);
	}
}
