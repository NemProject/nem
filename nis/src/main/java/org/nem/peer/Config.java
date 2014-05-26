package org.nem.peer;

import net.minidev.json.*;
import org.nem.core.serialization.*;
import org.nem.peer.node.*;
import org.nem.peer.trust.*;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents peer configuration.
 */
public class Config {

	private static final String DEFAULT_PLATFORM = "Unknown";
	private static final String DEFAULT_APPLICATION = "Unknown";

	private final Node localNode;
	private final PreTrustedNodes preTrustedNodes;
	private final TrustParameters trustParameters;
	private final TrustProvider trustProvider;

	/**
	 * Creates a new configuration object from a JSON configuration object.
	 *
	 * @param jsonConfig A JSON configuration object.
	 */
	public Config(final JSONObject jsonConfig) {
		if (!jsonConfig.containsKey("platform")) {
			final String defaultPlatform = String.format(
					"%s (%s) on %s",
					System.getProperty("java.vendor"),
					System.getProperty("java.version"),
					System.getProperty("os.name"));
			jsonConfig.put("platform", defaultPlatform);
		}

		final JsonDeserializer deserializer = new JsonDeserializer(jsonConfig, new DeserializationContext(null));
		this.localNode = parseLocalNode(deserializer);
		this.preTrustedNodes = parseWellKnownPeers(deserializer);
		this.trustParameters = getDefaultTrustParameters();
		this.trustProvider = getDefaultTrustProvider();
	}

	/**
	 * Loads configuration from a file.
	 *
	 * @param configFileName The configuration file name.
	 *
	 * @return The configuration.
	 */
	public static Config fromFile(final String configFileName) {
		try {
			try (final InputStream fin = Config.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin)
					throw new FatalConfigException(String.format("Configuration file <%s> not available", configFileName));

				return new Config((JSONObject)JSONValue.parse(fin));
			}
		} catch (Exception e) {
			throw new FatalConfigException("Exception encountered while loading config", e);
		}
	}

	/**
	 * Gets the network name.
	 *
	 * @return The network name.
	 */
	public String getNetworkName() {
		return "Default Network";
	}

	/**
	 * Gets the local node.
	 *
	 * @return The local node.
	 */
	public Node getLocalNode() {
		return this.localNode;
	}

	/**
	 * Gets all pre-trusted nodes.
	 *
	 * @return The pre-trusted nodes.
	 */
	public PreTrustedNodes getPreTrustedNodes() {
		return this.preTrustedNodes;
	}

	/**
	 * Gets the trust parameters.
	 *
	 * @return The trust parameters.
	 */
	public TrustParameters getTrustParameters() {
		return this.trustParameters;
	}

	/**
	 * Gets the trust provider.
	 *
	 * @return The trust provider.
	 */
	public TrustProvider getTrustProvider() {
		return this.trustProvider;
	}

	private static Node parseLocalNode(final Deserializer deserializer) {
		return new Node(deserializer);
	}

	private static PreTrustedNodes parseWellKnownPeers(final Deserializer deserializer) {
		final List<NodeEndpoint> wellKnownEndpoints = deserializer.readObjectArray("knownPeers", NodeEndpoint.DESERIALIZER);

		final Set<Node> wellKnownNodes = wellKnownEndpoints.stream()
				.map(endpoint -> new Node(endpoint, DEFAULT_PLATFORM, DEFAULT_APPLICATION))
				.collect(Collectors.toSet());

		return new PreTrustedNodes(wellKnownNodes);
	}

	private static TrustParameters getDefaultTrustParameters() {
		final TrustParameters params = new TrustParameters();
		params.set("MAX_ITERATIONS", "10");
		params.set("ALPHA", "0.05");
		params.set("EPSILON", "0.001");
		return params;
	}

	private static TrustProvider getDefaultTrustProvider() {
		final int LOW_COMMUNICATION_NODE_WEIGHT = 30;
		return new LowComTrustProvider(new EigenTrustPlusPlus(), LOW_COMMUNICATION_NODE_WEIGHT);
	}

	/**
	 * A fatal configuration exception.
	 */
	private static class FatalConfigException extends RuntimeException {
		/**
		 * Creates a new config exception.
		 *
		 * @param message The exception message.
		 */
		public FatalConfigException(final String message) {
			super(message);
		}

		/**
		 * Creates a new config exception.
		 *
		 * @param message The exception message.
		 * @param cause   The original exception.
		 */
		public FatalConfigException(final String message, Throwable cause) {
			super(message, cause);
		}
	}
}
