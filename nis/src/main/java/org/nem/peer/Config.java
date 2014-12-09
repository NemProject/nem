package org.nem.peer;

import net.minidev.json.JSONObject;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.peer.trust.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents peer configuration.
 */
public class Config {

	private final Node localNode;
	private final PreTrustedNodes preTrustedNodes;
	private final TrustParameters trustParameters;
	private final TrustProvider trustProvider;

	/**
	 * Creates a new configuration object.
	 *
	 * @param localNode The local node.
	 * @param peersConfig A JSON object containing peer settings.
	 * @param applicationVersion The application version.
	 */
	public Config(final Node localNode, final JSONObject peersConfig, final String applicationVersion) {
		this(
				localNode,
				parseWellKnownPeers(new JsonDeserializer(peersConfig, null)),
				getDefaultTrustParameters(),
				getDefaultTrustProvider(),
				applicationVersion);
	}

	/**
	 * Creates a new configuration object.
	 *
	 * @param localNode The local node.
	 * @param preTrustedNodes The pre-trusted nodes.
	 * @param trustParameters The trust parameters.
	 * @param trustProvider The trust provider.
	 * @param applicationVersion The application version.
	 */
	public Config(
			final Node localNode,
			final PreTrustedNodes preTrustedNodes,
			final TrustParameters trustParameters,
			final TrustProvider trustProvider,
			final String applicationVersion) {
		this.updateLocalNodeMetaData(localNode, applicationVersion);
		this.localNode = localNode;
		this.preTrustedNodes = preTrustedNodes;
		this.trustParameters = trustParameters;
		this.trustProvider = trustProvider;
	}

	private void updateLocalNodeMetaData(final Node localNode, final String applicationVersion) {
		String platform = localNode.getMetaData().getPlatform();
		if (null == platform) {
			platform = String.format(
					"%s (%s) on %s",
					System.getProperty("java.vendor"),
					System.getProperty("java.version"),
					System.getProperty("os.name"));
		}

		final NodeMetaData metaData = new NodeMetaData(
				platform,
				localNode.getMetaData().getApplication(),
				NodeVersion.parse(applicationVersion));
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

	/**
	 * Gets the trust provider.
	 *
	 * @return The trust provider.
	 */
	public TrustProvider getTrustProvider() {
		return this.trustProvider;
	}

	private static PreTrustedNodes parseWellKnownPeers(final Deserializer deserializer) {
		final List<Node> wellKnownNodes = deserializer.readOptionalObjectArray("knownPeers", Config::activate);
		final Set<Node> preTrustedNodes = new HashSet<>();
		if (null != wellKnownNodes) {
			preTrustedNodes.addAll(wellKnownNodes.stream().filter(n -> null != n).collect(Collectors.toList()));
		}

		return new PreTrustedNodes(preTrustedNodes);
	}

	private static Node activate(final Deserializer obj) {
		try {
			return new Node(obj);
		} catch (Exception e) {
			return null;
		}
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
}
