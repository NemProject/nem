package org.nem.peer;

import net.minidev.json.*;
import org.nem.core.serialization.*;
import org.nem.peer.trust.PreTrustedNodes;

import java.util.*;

/**
 * Represents peer configuration.
 */
public class Config {

    private static final String DEFAULT_PLATFORM = "Unknown";
    private static final String DEFAULT_APPLICATION = "Unknown";

    private final Node localNode;
    private final PreTrustedNodes preTrustedNodes;

    /**
     * Creates a new configuration object from a JSON configuration object.
     *
     * @param jsonConfig A JSON configuration object.
     */
    public Config(final JSONObject jsonConfig) {
        final JsonDeserializer deserializer = new JsonDeserializer(jsonConfig, new DeserializationContext(null));
        this.localNode = parseLocalNode(deserializer);
        this.preTrustedNodes = parseWellKnownPeers(deserializer);
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
     * Gets all pre-trusted nodes.
     *
     * @return The pre-trusted nodes.
     */
    public PreTrustedNodes getPreTrustedNodes() { return this.preTrustedNodes; }

    private static Node parseLocalNode(final Deserializer deserializer) {
        return new Node(deserializer);
    }

    private static PreTrustedNodes parseWellKnownPeers(final Deserializer deserializer) {
        final List<NodeEndpoint> wellKnownEndpoints = deserializer.readObjectArray("knownPeers", NodeEndpoint.DESERIALIZER);

        final Set<Node> wellKnownNodes = new HashSet<>();
        for (final NodeEndpoint endpoint : wellKnownEndpoints)
            wellKnownNodes.add(new Node(endpoint, DEFAULT_PLATFORM, DEFAULT_APPLICATION));

        return new PreTrustedNodes(wellKnownNodes);
    }
}
