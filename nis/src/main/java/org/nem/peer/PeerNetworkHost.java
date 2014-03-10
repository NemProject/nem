package org.nem.peer;

import net.minidev.json.*;
import org.nem.peer.net.HttpPeerConnector;

import java.io.InputStream;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * The infrastructure class that actually runs the peer network.
 */
public class PeerNetworkHost implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());
    private static final PeerNetworkHost DEFAULT_HOST = new PeerNetworkHost("peers-config.json");

    /**
     * Gets the default peer network host.
     *
     * @return The default peer network host.
     */
    public static PeerNetworkHost getDefaultHost() { return DEFAULT_HOST; }

    private final PeerNetwork network;
    private final ScheduledThreadPoolExecutor peerListRefresherExecutor;

    /**
     * Creates a host that hosts a specified network.
     *
     * @param configFileName The network configuration file name.
     */
    public PeerNetworkHost(final String configFileName) {
        this(new PeerNetwork(loadConfig(configFileName), new HttpPeerConnector()), 200, 10000);
    }

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

    private static Config loadConfig(final String configFileName) {
        try {
            try (final InputStream fin = PeerNetwork.class.getClassLoader().getResourceAsStream(configFileName)) {
                if (null == fin)
                    throw new FatalPeerException(String.format("Configuration file <%s> not available", configFileName));

                return new Config((JSONObject)JSONValue.parse(fin));
            }
        }
        catch (Exception e) {
            throw new FatalPeerException("Exception encountered while loading config", e);
        }
    }
}
