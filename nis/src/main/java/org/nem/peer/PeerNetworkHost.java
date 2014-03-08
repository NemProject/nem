package org.nem.peer;

import net.minidev.json.*;

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
    public static final PeerNetworkHost getDefaultHost() { return DEFAULT_HOST; }

    private final PeerNetwork network;
    private final ScheduledThreadPoolExecutor peerListRefresherExecutor;

    /**
     * Creates a host that hosts a specified network.
     *
     * @param configFileName The network configuration file name.
     */
    public PeerNetworkHost(final String configFileName) {
        final Config config = loadConfig(configFileName);

        this.network = new PeerNetwork(config, new HttpPeerConnector());

        this.peerListRefresherExecutor = new ScheduledThreadPoolExecutor(1);
        this.peerListRefresherExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Start refreshing peer list.");
                network.refresh();
            }
        }, 2, 10, TimeUnit.SECONDS);
    }

    /**
     * Gets the hosted network.
     *
     * @return The hosted network.
     */
    public PeerNetwork getNetwork() { return this.network; }

    @Override
    public void close() {
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
