package org.nem.peer;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.peer.node.*;
import org.nem.peer.test.*;
import org.nem.peer.trust.*;

import java.net.*;
import java.util.*;

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
	public void localNodeIsInitializedCorrectly() throws Exception {
		// Arrange:
		final Config config = createTestConfig();

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		Assert.assertThat(node.getEndpoint().getBaseUrl(), IsEqual.equalTo(getDefaultLocalNodeUrl()));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo("Mac"));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("FooBar"));
	}

	@Test
	public void localNodeIsGivenDefaultPlatformIfOmitted() throws Exception {
		// Arrange:
		final JSONObject jsonConfig = ConfigFactory.createTestJsonConfig();
		jsonConfig.remove("platform");
		final Config config = new Config(jsonConfig);

		// Act:
		final Node node = config.getLocalNode();

		// Assert:
		final String expectedPlatform = String.format(
				"%s (%s) on %s",
				System.getProperty("java.vendor"),
				System.getProperty("java.version"),
				System.getProperty("os.name"));
		Assert.assertThat(node.getEndpoint().getBaseUrl(), IsEqual.equalTo(getDefaultLocalNodeUrl()));
		Assert.assertThat(node.getPlatform(), IsEqual.equalTo(expectedPlatform));
		Assert.assertThat(node.getVersion(), IsEqual.equalTo(2));
		Assert.assertThat(node.getApplication(), IsEqual.equalTo("FooBar"));
	}

	@Test
	public void wellKnownPeersAreInitializedCorrectly() {
		// Arrange:
		final String[] knownHosts = new String[] { "10.0.0.5", "10.0.0.12", "10.0.0.3" };
		final Config config = new Config(ConfigFactory.createTestJsonConfig(knownHosts));

		// Act:
		final PreTrustedNodes preTrustedNodes = config.getPreTrustedNodes();
		final Set<Node> wellKnownPeers = preTrustedNodes.getNodes();

		// Assert:
		Assert.assertThat(preTrustedNodes.getSize(), IsEqual.equalTo(3));
		Assert.assertThat(wellKnownPeers.size(), IsEqual.equalTo(3));
		Assert.assertThat(wellKnownPeers.contains(createConfigNode("10.0.0.5")), IsEqual.equalTo(true));
		Assert.assertThat(wellKnownPeers.contains(createConfigNode("10.0.0.12")), IsEqual.equalTo(true));
		Assert.assertThat(wellKnownPeers.contains(createConfigNode("10.0.0.3")), IsEqual.equalTo(true));
	}

	@Test
	public void wellKnownPeersAreEmptyIfNotSpecified() {
		// Arrange:
		final JSONObject jsonConfig = ConfigFactory.createTestJsonConfig();
		jsonConfig.remove("knownPeers");
		final Config config = new Config(jsonConfig);

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

	private static Node createConfigNode(final String host) {
		return new Node(new NodeEndpoint("ftp", host, 12), "plat", "app");
	}

	private static Config createTestConfig() {
		return ConfigFactory.createDefaultTestConfig();
	}

	private static URL getDefaultLocalNodeUrl() throws MalformedURLException {
		return new URL("http", DEFAULT_LOCAL_NODE_HOST, 7890, "/");
	}

	//endregion
}
