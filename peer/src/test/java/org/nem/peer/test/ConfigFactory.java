package org.nem.peer.test;

import net.minidev.json.*;
import org.nem.core.crypto.*;
import org.nem.core.node.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.Base64Encoder;
import org.nem.peer.Config;

import java.math.BigInteger;

/**
 * Static class containing utility functions for creating Config objects.
 */
public class ConfigFactory {

	/**
	 * The default local node host.
	 */
	public static final String DEFAULT_LOCAL_NODE_HOST = "10.0.0.8";

	/**
	 * Creates default local node configuration.
	 *
	 * @return The configuration.
	 */
	public static Node createDefaultLocalNode() {
		final byte[] privateKeyBytes = Base64Encoder.getBytes("Dnumq0AdMXpLbaE1F1VYMLbDw6wG3wHvj67uWLrpy5A=");
		final PrivateKey privateKey = new PrivateKey(new BigInteger(privateKeyBytes));

		// note that the Config constructor parameter should take precedence over the meta data version
		return new Node(new NodeIdentity(new KeyPair(privateKey), "local larry"), new NodeEndpoint("http", DEFAULT_LOCAL_NODE_HOST, 7890),
				new NodeMetaData("default-cf-plat", "default-cf-app"));
	}

	/**
	 * Creates default peers configuration.
	 *
	 * @param hosts The peer hosts.
	 * @return The configuration.
	 */
	public static JSONObject createDefaultPeersConfig(final String[] hosts) {
		final JSONObject jsonConfig = new JSONObject();
		jsonConfig.put("knownPeers", createKnownPeersJson(hosts));
		return jsonConfig;
	}

	/**
	 * Creates peers configuration with an unresolvable host.
	 *
	 * @param resolvableHosts The resolvable hosts.
	 * @return The configuration.
	 */
	public static JSONObject createPeersConfigWithUnresolvableHost(final String[] resolvableHosts) {
		final JSONArray jsonWellKnownPeers = createKnownPeersJson(resolvableHosts);
		jsonWellKnownPeers.add(createUnresolvableHost());

		final JSONObject jsonConfig = new JSONObject();
		jsonConfig.put("knownPeers", jsonWellKnownPeers);
		return jsonConfig;
	}

	/**
	 * Creates default peers configuration with three hosts.
	 *
	 * @return The configuration.
	 */
	public static JSONObject createDefaultPeersConfig() {
		return createDefaultPeersConfig(new String[]{
				"10.0.0.1", "10.0.0.3", "10.0.0.2"
		});
	}

	/**
	 * Creates a default Config object that can be used in tests.
	 *
	 * @return A default Config object.
	 */
	public static Config createDefaultTestConfig() {
		return new Config(createDefaultLocalNode(), createDefaultPeersConfig(), "2.0.0", 4, new NodeFeature[]{
				NodeFeature.HISTORICAL_ACCOUNT_DATA, NodeFeature.PLACEHOLDER2
		});
	}

	private static JSONObject createEndpointJsonObject(final String protocol, final String host, final int port) {
		final JSONObject jsonEndpoint = new JSONObject();
		jsonEndpoint.put("protocol", protocol);
		jsonEndpoint.put("host", host);
		jsonEndpoint.put("port", port);
		return jsonEndpoint;
	}

	private static JSONObject createIdentityJsonObject(final String publicKey) {
		final JSONObject jsonIdentity = new JSONObject();
		jsonIdentity.put("public-key", publicKey);
		return jsonIdentity;
	}

	private static JSONObject createUnresolvableHost() {
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final JSONObject jsonWellKnownPeer = new JSONObject();
		jsonWellKnownPeer.put("endpoint", createEndpointJsonObject("ftp", "HiIAmAlice", 12));
		jsonWellKnownPeer.put("identity", createIdentityJsonObject(publicKey.toString()));
		return jsonWellKnownPeer;
	}

	private static JSONObject createNodeJson(final String hostName) {
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final JSONObject jsonNode = new JSONObject();
		jsonNode.put("endpoint", createEndpointJsonObject("ftp", hostName, 12));
		jsonNode.put("identity", createIdentityJsonObject(publicKey.toString()));
		return jsonNode;
	}

	private static JSONArray createKnownPeersJson(final String[] hosts) {
		final JSONArray jsonWellKnownPeers = new JSONArray();
		for (final String hostName : hosts) {
			jsonWellKnownPeers.add(createNodeJson(hostName));
		}

		return jsonWellKnownPeers;
	}
}
