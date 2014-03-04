package org.nem.peer.v2;

import net.minidev.json.*;

import java.util.*;

/**
 * Represents peer configuration.
 */
public class Config {

    private final Node localNode;
    private final Set<String> wellKnownPeers;

    /**
     * Creates a new configuration object from a JSON configuration object.
     *
     * @param applicationName The name of the application.
     * @param jsonConfig A JSON configuration object.
     */
    public Config(final String applicationName, final JSONObject jsonConfig) {

        NodeInfo info = new NodeInfo(
            new LocalNodeAddress((String)jsonConfig.get("myAddress")),
            (String)jsonConfig.get("myPlatform"),
            applicationName);
        this.localNode = new Node(info);

        this.wellKnownPeers = parseWellKnownPeers((JSONArray)jsonConfig.get("knownPeers"));
    }

    /**
     * Gets the network name.
     *
     * @return The network name.
     */
    public String getNetworkName() { return "Default Network"; }

    /**
     * Gets the local node.
     *
     * @return The local node.
     */
    public Node getLocalNode() { return this.localNode; }

    /**
     * Gets the set of well known peers.
     *
     * @return The set of well known peers.
     */
    public Set<String> getWellKnownPeers() { return this.wellKnownPeers; }

    private static Set<String> parseWellKnownPeers(final JSONArray jsonWellKnownPeers) {
        if (null == jsonWellKnownPeers)
            return Collections.emptySet();

        Set<String> wellKnownPeers = new HashSet<>();
        for (int i = 0; i < jsonWellKnownPeers.size(); ++i) {
            String host = ((String)jsonWellKnownPeers.get(i)).trim();
            if (!host.isEmpty())
                wellKnownPeers.add(host);
        }

        return Collections.unmodifiableSet(wellKnownPeers);
    }
}
