package org.nem.peer;

import net.minidev.json.JSONObject;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.peer.trust.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents peer configuration.
 */
public class Config {
	private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

	private final Node localNode;
	private final PreTrustedNodes preTrustedNodes;
	private final TrustParameters trustParameters;

	/**
	 * Creates a new configuration object.
	 *
	 * @param localNode The local node.
	 * @param peersConfig A JSON object containing peer settings.
	 * @param applicationVersion The application version.
	 * @param networkId The network id.
	 * @param localNodeFeatures The features supported by the local node.
	 */
	public Config(final Node localNode, final JSONObject peersConfig, final String applicationVersion, final int networkId,
			final NodeFeature[] localNodeFeatures) {
		this(localNode, parseWellKnownPeers(new JsonDeserializer(peersConfig, null)), null, applicationVersion, networkId,
				localNodeFeatures);
	}

	/**
	 * Creates a new configuration object.
	 *
	 * @param localNode The local node.
	 * @param preTrustedNodes The pre-trusted nodes.
	 * @param trustParameters The trust parameters.
	 * @param applicationVersion The application version.
	 * @param networkId The network id.
	 * @param localNodeFeatures The features supported by the local node.
	 */
	public Config(final Node localNode, final PreTrustedNodes preTrustedNodes, final TrustParameters trustParameters,
			final String applicationVersion, final int networkId, final NodeFeature[] localNodeFeatures) {
		final NodeVersion nodeVersion = NodeVersion.parse(applicationVersion);
		if (0 == preTrustedNodes.getSize() && !"DEVELOPER BUILD".equals(nodeVersion.getTag())) {
			throw new IllegalArgumentException("set of pre-trusted nodes cannot be empty");
		}

		this.localNode = localNode;
		this.preTrustedNodes = preTrustedNodes;
		this.trustParameters = null == trustParameters ? getDefaultTrustParameters() : trustParameters;

		String platform = localNode.getMetaData().getPlatform();
		if (null == platform) {
			platform = String.format("%s (%s) on %s", System.getProperty("java.vendor"), System.getProperty("java.version"),
					System.getProperty("os.name"));
		}

		final NodeMetaData metaData = new NodeMetaData(platform, localNode.getMetaData().getApplication(), nodeVersion, networkId,
				NodeFeature.or(localNodeFeatures));
		localNode.setMetaData(metaData);
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

	private static PreTrustedNodes parseWellKnownPeers(final Deserializer deserializer) {
		final List<Node> wellKnownNodes = deserializer.readOptionalObjectArray("knownPeers", d -> {
			try {
				return new Node(d);
			} catch (final InvalidNodeEndpointException e) {
				LOGGER.warning(String.format("Skipping invalid known peer: %s", e));
				return null;
			}
		});

		final Set<Node> preTrustedNodes = new HashSet<>();
		if (null != wellKnownNodes) {
			preTrustedNodes.addAll(wellKnownNodes.stream().filter(n -> null != n).collect(Collectors.toList()));
		}

		return new PreTrustedNodes(preTrustedNodes);
	}

	private static TrustParameters getDefaultTrustParameters() {
		final TrustParameters params = new TrustParameters();
		params.set("MAX_ITERATIONS", "20");
		params.set("ALPHA", "0.1");
		params.set("EPSILON", "0.01");
		return params;
	}
}
