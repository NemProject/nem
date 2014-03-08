package org.nem.peer.v2;

import net.minidev.json.*;

/**
 * Static class containing utility functions that are used by multiple peer test suites.
 */
public class PeerTestUtils {

    //region config

    /**
     * Creates a JSONObject that represents an endpoint.
     *
     * @param protocol The protocol.
     * @param host The host.
     * @param port The port.
     * @return A JSONObject that represents an endpoint.
     */
    public static JSONObject createEndpointJsonObject(final String protocol, final String host, final int port) {
        JSONObject jsonEndpoint = new JSONObject();
        jsonEndpoint.put("protocol", protocol);
        jsonEndpoint.put("host", host);
        jsonEndpoint.put("port", port);
        return jsonEndpoint;
    }

    /**
     * Creates a JSONObject that represents a network configuration.
     *
     * @param hosts The hosts that should be used as well known peers.
     * @return A JSONObject that represents a network configuration.
     */
    public static JSONObject createTestJsonConfig(final String[] hosts) {
        JSONObject jsonConfig = new JSONObject();

        jsonConfig.put("endpoint", createEndpointJsonObject("http", "10.0.0.8", 7890));

        jsonConfig.put("platform", "Mac");
        jsonConfig.put("application", "FooBar");

        JSONArray jsonWellKnownPeers = new JSONArray();
        for (final String hostName : hosts)
            jsonWellKnownPeers.add(createEndpointJsonObject("ftp", hostName, 12));

        jsonConfig.put("knownPeers", jsonWellKnownPeers);
        return jsonConfig;
    }

    /**
     * Creates a JSONObject that represents a network configuration and has three
     * well-known hosts.
     *
     * @return A JSONObject that represents a network configuration.
     */
    public static JSONObject createTestJsonConfig() {
        return createTestJsonConfig(new String[] { "10.0.0.1", "10.0.0.3", "10.0.0.2" });
    }

    /**
     * Creates a default Config object that can be used in tests.
     *
     * @return A default Config object.
     */
    public static Config createDefaultTestConfig() {
        return new Config(createTestJsonConfig());
    }

    //endregion
}
