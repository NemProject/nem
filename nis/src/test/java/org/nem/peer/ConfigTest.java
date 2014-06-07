package org.nem.peer;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.IsEquivalent;
import org.nem.peer.node.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.*;

import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigTest {

	private static final String DEFAULT_LOCAL_NODE_HOST = ConfigFactory.DEFAULT_LOCAL_NODE_HOST;

	@Test
	public void networkNameIsInitializedCorrectly() {
		// Arrange:
		final Config config = createTestConfig();

		// Assert:
		Assert.assertThat(config.getNetworkName(), IsEqual.equalTo("Default Network"));
	}

	@Test
	public void localNodeIsInitializedCorrectly() {
		// Arrange:
		final Config config = createTestConfig();

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		assertLocalNodeProperties(node, "Mac");
	}

	@Test
	public void localNodeIsGivenDefaultPlatformIfOmitted() {
		// Arrange:
		final JSONObject localConfig = ConfigFactory.createDefaultLocalConfig();
		((JSONObject)localConfig.get("metaData")).remove("platform");
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig();
		final Config config = new Config(localConfig, peersConfig, "2.0");

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		final String expectedPlatform = String.format(
				"%s (%s) on %s",
				System.getProperty("java.vendor"),
				System.getProperty("java.version"),
				System.getProperty("os.name"));
		assertLocalNodeProperties(node, expectedPlatform);
	}

	private static void assertLocalNodeProperties(final Node node, final String expectedPlatform) {
		final NodeMetaData metaData = node.getMetaData();
		Assert.assertThat(node.getIdentity().getName(), IsEqual.equalTo("local larry"));
		Assert.assertThat(node.getIdentity().isOwned(), IsEqual.equalTo(true));
		Assert.assertThat(node.getEndpoint().getBaseUrl().getHost(), IsEqual.equalTo(DEFAULT_LOCAL_NODE_HOST));
		Assert.assertThat(metaData.getPlatform(), IsEqual.equalTo(expectedPlatform));
		Assert.assertThat(metaData.getVersion(), IsEqual.equalTo("2.0"));
		Assert.assertThat(metaData.getApplication(), IsEqual.equalTo("FooBar"));
	}

	@Test
	public void wellKnownPeersAreInitializedCorrectly() {
		// Arrange:
		final JSONObject localConfig = ConfigFactory.createDefaultLocalConfig();
		final String[] expectedWellKnownHosts = new String[] { "10.0.0.5", "10.0.0.12", "10.0.0.3" };
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig(expectedWellKnownHosts);
		final Config config = new Config(localConfig, peersConfig, "2.0");

		// Act:
		final PreTrustedNodes preTrustedNodes = config.getPreTrustedNodes();
		final List<String> wellKnownPeers = preTrustedNodes.getNodes().stream()
				.map(node -> node.getEndpoint().getBaseUrl().getHost())
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
		Assert.assertThat(wellKnownPeers, IsEquivalent.equivalentTo(expectedWellKnownHosts));
	}

	@Test
	public void wellKnownPeersAreEmptyIfNotSpecified() {
		// Arrange:
		final JSONObject localConfig = ConfigFactory.createDefaultLocalConfig();
		final JSONObject peersConfig = ConfigFactory.createDefaultPeersConfig();
		peersConfig.remove("knownPeers");
		final Config config = new Config(localConfig, peersConfig, "2.0");

		// Act:
		final PreTrustedNodes preTrustedNodes = config.getPreTrustedNodes();
		final Set<Node> wellKnownPeers = preTrustedNodes.getNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(0));
		Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(0));
	}

	@Test
	public void trustParametersAreInitializedWithDefaultValues() {
		// Arrange:
		final Config config = createTestConfig();

		// Act:
		final TrustParameters params = config.getTrustParameters();

		// Assert:
		Assert.assertThat(params.getAsInteger("MAX_ITERATIONS"), IsEqual.equalTo(10));
		Assert.assertThat(params.getAsDouble("ALPHA"), IsEqual.equalTo(0.05));
		Assert.assertThat(params.getAsDouble("EPSILON"), IsEqual.equalTo(0.001));
	}

	@Test
	public void trustProviderIsInitialized() {
		// Arrange:
		final Config config = createTestConfig();

		// Act:
		final TrustProvider provider = config.getTrustProvider();

		// Assert:
		Assert.assertThat(provider, IsNull.notNullValue());
	}

	//region Factories

	private static Config createTestConfig() {
		return ConfigFactory.createDefaultTestConfig();
	}

	private static URL getDefaultLocalNodeUrl() throws MalformedURLException {
		return new URL("http", DEFAULT_LOCAL_NODE_HOST, 7890, "/");
	}

	//endregion
}
