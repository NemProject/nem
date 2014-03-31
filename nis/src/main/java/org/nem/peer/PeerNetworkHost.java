package org.nem.peer;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * The infrastructure class that actually runs the peer network.
 */
public class PeerNetworkHost implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());

    private final PeerNetwork network;
    private final ScheduledThreadPoolExecutor peerListRefresherExecutor;

    /**
     * Creates a host that hosts the specified network.
     *
     * @param network The network.
     * @param refreshInitialDelay The initial delay before starting the refresh loop (in ms).
     * @param refreshInterval The refresh interval (in ms).
     */
    public PeerNetworkHost(final PeerNetwork network, int refreshInitialDelay, int refreshInterval) {
        this.network = network;

        this.peerListRefresherExecutor = new ScheduledThreadPoolExecutor(1);
        this.peerListRefresherExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Refreshing network");
                network.refresh();
                network.broadcast(NodeApiId.REST_NODE_PING, network.getLocalNodeAndExperiences());
				network.synchronize();
            }
        }, refreshInitialDelay, refreshInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the hosted network.
     *
     * @return The hosted network.
     */
    public PeerNetwork getNetwork() { return this.network; }

    @Override
    public void close() {
        LOGGER.info("Stopping network refresh thread");
        this.peerListRefresherExecutor.shutdownNow();
    }
}
