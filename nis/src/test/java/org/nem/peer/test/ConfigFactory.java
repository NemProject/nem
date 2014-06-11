package org.nem.peer.test;

import net.minidev.json.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.test.Utils;
import org.nem.core.utils.Base64Encoder;
import org.nem.peer.Config;

/**
 * Static class containing utility functions for creating Config objects.
 */
public class ConfigFactory {

	/**
	 * The default local node host.
	 */
	public static final String DEFAULT_LOCAL_NODE_HOST = "10.0.0.8";

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

	private static JSONObject createIdentityJsonObjectWithPrivateKey(final String privateKey) {
		final JSONObject jsonIdentity = new JSONObject();
		jsonIdentity.put("private-key", privateKey);
		jsonIdentity.put("name", "local larry");
		return jsonIdentity;
	}

	/**
	 * Creates default local node configuration.
	 *
	 * @return The configuration.
	 */
	public static JSONObject createDefaultLocalConfig() {
		final JSONObject jsonConfig = new JSONObject();

		jsonConfig.put("endpoint", createEndpointJsonObject("http", DEFAULT_LOCAL_NODE_HOST, 7890));
		jsonConfig.put("identity", createIdentityJsonObjectWithPrivateKey("Dnumq0AdMXpLbaE1F1VYMLbDw6wG3wHvj67uWLrpy5A="));

		final JSONObject jsonMetaData = new JSONObject();
		jsonMetaData.put("version", "1.0"); // note that the Config constructor parameter should take precedence
		jsonMetaData.put("platform", "Mac");
		jsonMetaData.put("application", "FooBar");
		jsonConfig.put("metaData", jsonMetaData);
		return jsonConfig;
	}

	/**
	 * Creates default peers configuration.
	 *
	 * @param hosts The peer hosts.
	 * @return The configuration.
	 */
	public static JSONObject createDefaultPeersConfig(final String[] hosts) {
		final JSONObject jsonConfig = new JSONObject();
		final JSONArray jsonWellKnownPeers = new JSONArray();
		for (final String hostName : hosts) {
			final PublicKey publicKey = Utils.generateRandomPublicKey();
			final JSONObject jsonWellKnownPeer = new JSONObject();
			jsonWellKnownPeer.put("endpoint", createEndpointJsonObject("ftp", hostName, 12));
			jsonWellKnownPeer.put("identity", createIdentityJsonObject(Base64Encoder.getString(publicKey.getRaw())));
			jsonWellKnownPeers.add(jsonWellKnownPeer);
		}

		jsonConfig.put("knownPeers", jsonWellKnownPeers);
		return jsonConfig;
	}

	/**
	 * Creates default peers configuration with three hosts.
	 *
	 * @return The configuration.
	 */
	public static JSONObject createDefaultPeersConfig() {
		return createDefaultPeersConfig(new String[] { "10.0.0.1", "10.0.0.3", "10.0.0.2" });
	}

	/**
	 * Creates a default Config object that can be used in tests.
	 *
	 * @return A default Config object.
	 */
	public static Config createDefaultTestConfig() {
		return new Config(
				createDefaultLocalConfig(),
				createDefaultPeersConfig(),
				"2.0");
	}
}
