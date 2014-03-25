package org.nem.peer;

import net.minidev.json.*;
import org.nem.core.serialization.*;

import java.io.InputStream;
import java.util.*;

/**
 * Represents peer configuration.
 */
public class Config {

    private final Node localNode;
    private final Set<NodeEndpoint> wellKnownPeers;

    /**
     * Creates a new configuration object from a JSON configuration object.
     *
     * @param jsonConfig A JSON configuration object.
     */
    public Config(final JSONObject jsonConfig) {
        final JsonDeserializer deserializer = new JsonDeserializer(jsonConfig, new DeserializationContext(null));
        this.localNode = parseLocalNode(deserializer);
        this.wellKnownPeers = parseWellKnownPeers(deserializer);
    }

	public static Config loadConfig(final String configFileName) {
		try {
			try (final InputStream fin = Config.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin)
					throw new FatalConfigExeception(String.format("Configuration file <%s> not available", configFileName));

				return new Config((JSONObject)JSONValue.parse(fin));
			}
		}
		catch (Exception e) {
			throw new FatalConfigExeception("Exception encountered while loading config", e);
		}
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
    public Set<NodeEndpoint> getWellKnownPeers() { return this.wellKnownPeers; }

    private static Node parseLocalNode(final Deserializer deserializer) {
        return new Node(deserializer);
    }

    private static Set<NodeEndpoint> parseWellKnownPeers(final Deserializer deserializer) {
        final List<NodeEndpoint> wellKnownPeers = deserializer.readObjectArray("knownPeers", NodeEndpoint.DESERIALIZER);
        return Collections.unmodifiableSet(new HashSet<>(wellKnownPeers));
    }

	static class FatalConfigExeception extends RuntimeException {
		/**
		 * Creates a new config exception.
		 *
		 * @param message The exception message.
		 */
		public FatalConfigExeception(final String message) {
			super(message);
		}

		/**
		 * Creates a new config exception.
		 *
		 * @param cause The exception message.
		 */
		public FatalConfigExeception(Throwable cause) {
			super(cause);
		}

		/**
		 * Creates a new config exception.
		 *
		 * @param message The exception message.
		 * @param cause The original exception.
		 */
		public FatalConfigExeception(final String message, Throwable cause) {
			super(message, cause);
		}

	}
}
