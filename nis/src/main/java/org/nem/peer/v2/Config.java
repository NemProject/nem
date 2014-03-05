package org.nem.peer.v2;

import net.minidev.json.*;
import org.nem.core.serialization.*;

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
     * @param jsonConfig A JSON configuration object.
     */
    public Config(final JSONObject jsonConfig) {
        this.localNode = parseLocalNode(jsonConfig);
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

    private static Node parseLocalNode(final JSONObject jsonConfig) {
        JsonDeserializer deserializer = new JsonDeserializer(jsonConfig, new DeserializationContext(null));
        NodeInfo info = new NodeInfo(deserializer);
        return new Node(info);
    }

    private static Set<String> parseWellKnownPeers(final JSONArray jsonWellKnownPeers) {
        if (null == jsonWellKnownPeers)
            return Collections.emptySet();

        Set<String> wellKnownPeers = new HashSet<>();
        for (Object jsonWellKnownPeer : jsonWellKnownPeers) {
            String host = ((String)jsonWellKnownPeer).trim();
            if (!host.isEmpty())
                wellKnownPeers.add(host);
        }

        return Collections.unmodifiableSet(wellKnownPeers);
    }
}
