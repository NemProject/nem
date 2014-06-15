package org.nem.peer;

import net.minidev.json.*;
import org.nem.core.serialization.*;
import org.nem.peer.node.*;
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
		updateLocalNodeMetaData(localNode, applicationVersion);
		this.localNode = localNode;
		this.preTrustedNodes = parseWellKnownPeers(new JsonDeserializer(peersConfig, null));
		this.trustParameters = getDefaultTrustParameters();
		this.trustProvider = getDefaultTrustProvider();
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
				applicationVersion);
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

	private static Node parseLocalNode(final Deserializer deserializer) {
		return new LocalNodeDeserializer().deserialize(deserializer);
	}

	private static PreTrustedNodes parseWellKnownPeers(final Deserializer deserializer) {
		final List<Node> wellKnownNodes = deserializer.readObjectArray("knownPeers", Node::new);
		return new PreTrustedNodes(wellKnownNodes.stream().collect(Collectors.toSet()));
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
