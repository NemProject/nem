package org.nem.peer;

import net.minidev.json.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.node.*;
import org.nem.core.test.*;
import org.nem.peer.test.ConfigFactory;
import org.nem.peer.trust.*;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigTest {
	private static final String DEFAULT_LOCAL_NODE_HOST = ConfigFactory.DEFAULT_LOCAL_NODE_HOST;

	@Test
	public void networkNameIsInitializedCorrectly() {
		// Arrange:
		final Config config = createTestConfig();

		// Assert:
		MatcherAssert.assertThat(config.getNetworkName(), IsEqual.equalTo("Default Network"));
	}

	@Test
	public void localNodeIsInitializedCorrectly() {
		// Arrange:
		final Config config = createTestConfig();

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		assertLocalNodeProperties(node, "default-cf-plat", "default-cf-app");
	}

	@Test
	public void localNodeIsGivenDefaultPlatformIfOmitted() {
		// Arrange:
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		final NodeMetaData metaData = new NodeMetaData(null, localNode.getMetaData().getApplication());
		final Config config = createConfigWithCustomLocalNodeMetaData(metaData);

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		final String expectedPlatform = String.format("%s (%s) on %s", System.getProperty("java.vendor"),
				System.getProperty("java.version"), System.getProperty("os.name"));
		assertLocalNodeProperties(node, expectedPlatform, "default-cf-app");
	}

	@Test
	public void localNodeMetaDataCanOnlyOverridePlatformAndApplication() {
		final NodeMetaData metaData = new NodeMetaData("local-node-plat", "local-node-app", new NodeVersion(5, 4, 3), 4, 17);
		final Config config = createConfigWithCustomLocalNodeMetaData(metaData);

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		assertLocalNodeProperties(node, "local-node-plat", "local-node-app");
	}

	private static void assertLocalNodeProperties(final Node node, final String expectedPlatform, final String expectedApplication) {
		final NodeMetaData metaData = node.getMetaData();
		MatcherAssert.assertThat(node.getIdentity().getName(), IsEqual.equalTo("local larry"));
		MatcherAssert.assertThat(node.getIdentity().isOwned(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(node.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo(DEFAULT_LOCAL_NODE_HOST));
		MatcherAssert.assertThat(metaData.getPlatform(), IsEqual.equalTo(expectedPlatform));
		MatcherAssert.assertThat(metaData.getVersion(), IsEqual.equalTo(new NodeVersion(2, 0, 0)));
		MatcherAssert.assertThat(metaData.getApplication(), IsEqual.equalTo(expectedApplication));
		MatcherAssert.assertThat(metaData.getNetworkId(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(metaData.getFeaturesBitmask(), IsEqual.equalTo(6));
	}

	@Test
	public void wellKnownPeersAreInitializedCorrectly() {
		// Arrange:
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		final String[] expectedWellKnownHosts = new String[]{
				"10.0.0.5", "10.0.0.12", "10.0.0.3"
		};
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig(expectedWellKnownHosts);
		final Config config = createConfig(localNode, peersConfig, "2.0.0");

		// Act:
		final PreTrustedNodes preTrustedNodes = config.getPreTrustedNodes();
		final List<String> wellKnownPeers = preTrustedNodes.getNodes().stream().map(node -> node.getEndpoint().getBaseUrl().getHost())
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(wellKnownPeers, IsEquivalent.equivalentTo(expectedWellKnownHosts));
	}

	@Test
	public void cannotCreateConfigWithMissingWellKnownPeers() {
		// Arrange:
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig();
		peersConfig.remove("knownPeers");

		// Assert:
		ExceptionAssert.assertThrows(v -> new Config(localNode, peersConfig, "2.0.0", 0, new NodeFeature[]{}),
				IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateConfigWithEmptyWellKnownPeers() {
		// Arrange:
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig();
		peersConfig.put("knownPeers", new JSONArray());

		// Assert:
		ExceptionAssert.assertThrows(v -> new Config(localNode, peersConfig, "2.0.0", 0, new NodeFeature[]{}),
				IllegalArgumentException.class);
	}

	@Test
	public void canCreateConfigWithEmptyWellKnownPeersInDeveloperBuild() {
		// Arrange:
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig();
		peersConfig.put("knownPeers", new JSONArray());

		// Act:
		final Config config = createConfig(localNode, peersConfig, "2.0.0-DEVELOPER BUILD");

		// Assert:
		MatcherAssert.assertThat(config.getPreTrustedNodes().getSize(), IsEqual.equalTo(0));
	}

	@Test
	public void wellKnownPeersOmitUnresolvableHosts() {
		// Arrange:
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		final String[] expectedWellKnownHosts = new String[]{
				"10.0.0.1"
		};
		final JSONObject peersConfig = ConfigFactory.createPeersConfigWithUnresolvableHost(expectedWellKnownHosts);
		final Config config = createConfig(localNode, peersConfig, "2.0.0");

		// Act:
		final PreTrustedNodes preTrustedNodes = config.getPreTrustedNodes();
		final List<String> wellKnownPeers = preTrustedNodes.getNodes().stream().map(node -> node.getEndpoint().getBaseUrl().getHost())
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(wellKnownPeers, IsEquivalent.equivalentTo(expectedWellKnownHosts));
	}

	@Test
	public void trustParametersAreInitializedWithDefaultValues() {
		// Arrange:
		final Config config = createTestConfig();

		// Act:
		final TrustParameters params = config.getTrustParameters();

		// Assert:
		MatcherAssert.assertThat(params.getAsInteger("MAX_ITERATIONS"), IsEqual.equalTo(20));
		MatcherAssert.assertThat(params.getAsDouble("ALPHA"), IsEqual.equalTo(0.1));
		MatcherAssert.assertThat(params.getAsDouble("EPSILON"), IsEqual.equalTo(0.01));
	}

	// region Factories

	private static Config createTestConfig() {
		return ConfigFactory.createDefaultTestConfig();
	}

	private static Config createConfig(final Node localNode, final JSONObject peersConfig, final String applicationVersion) {
		return new Config(localNode, peersConfig, applicationVersion, 0, new NodeFeature[]{});
	}

	private static Config createConfigWithCustomLocalNodeMetaData(final NodeMetaData metaData) {
		final Node localNode = ConfigFactory.createDefaultLocalNode();
		localNode.setMetaData(metaData);

		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig();
		return new Config(localNode, peersConfig, "2.0.0", 4, new NodeFeature[]{
				NodeFeature.HISTORICAL_ACCOUNT_DATA, NodeFeature.PLACEHOLDER2
		});
	}

	// endregion
}
