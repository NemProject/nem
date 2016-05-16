package org.nem.deploy;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NetworkInfos;
import org.nem.core.node.NodeEndpoint;
import org.nem.core.test.*;

import java.nio.file.Paths;
import java.util.*;

public class CommonConfigurationTest {
	private static final List<String> REQUIRED_PROPERTY_NAMES = Arrays.asList(
			"nem.shortServerName",
			"nem.httpPort",
			"nem.httpsPort",
			"nem.webContext",
			"nem.apiContext",
			"nem.homePath",
			"nem.maxThreads");

	private static final List<String> OPTIONAL_PROPERTY_NAMES = Arrays.asList(
			"nem.websocketPort",
			"nem.folder",
			"nem.protocol",
			"nem.host",
			"nem.shutdownPath",
			"nem.useDosFilter",
			"nem.nonAuditedApiPaths",
			"nem.network");

	//region basic construction

	@Test
	public void canReadDefaultConfigurationFromResources() {
		// Act:
		final CommonConfiguration config = new CommonConfiguration();

		// Assert:
		assertDefaultRequiredConfiguration(config);
		assertDefaultOptionalConfiguration(config);
	}

	@Test
	public void canReadCustomConfiguration() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert
		assertCustomRequiredConfiguration(config);
		assertCustomOptionalConfiguration(config);
	}

	@Test
	public void canReadCustomConfigurationWithoutOptionalProperties() {
		// Arrange:
		final Properties properties = getCommonProperties();
		OPTIONAL_PROPERTY_NAMES.forEach(properties::remove);

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert
		assertCustomRequiredConfiguration(config);
		assertDefaultOptionalConfiguration(config);
	}

	private static void assertDefaultRequiredConfiguration(final CommonConfiguration config) {
		// Assert:
		Assert.assertThat(config.getShortServerName(), IsEqual.equalTo("Nis"));
		Assert.assertThat(config.getMaxThreads(), IsEqual.equalTo(500));
		Assert.assertThat(config.getHttpPort(), IsEqual.equalTo(7890));
		Assert.assertThat(config.getHttpsPort(), IsEqual.equalTo(7891));
		Assert.assertThat(config.getWebContext(), IsEqual.equalTo(""));
		Assert.assertThat(config.getApiContext(), IsEqual.equalTo(""));
		Assert.assertThat(config.getHomePath(), IsEqual.equalTo(""));
	}

	private static void assertDefaultOptionalConfiguration(final CommonConfiguration config) {
		// Assert:
		Assert.assertThat(config.getNemFolder(), IsEqual.equalTo(Paths.get(System.getProperty("user.home"), "nem").toString()));
		Assert.assertThat(config.getProtocol(), IsEqual.equalTo("http"));
		Assert.assertThat(config.getHost(), IsEqual.equalTo("127.0.0.1"));
		Assert.assertThat(config.getWebsocketPort(), IsEqual.equalTo(7778));
		Assert.assertThat(config.getShutdownPath(), IsEqual.equalTo("/shutdown"));
		Assert.assertThat(config.useDosFilter(), IsEqual.equalTo(true));

		final String[] expectedNonAuditedApiPaths = new String[] {
				"/heartbeat",
				"/status",
				"/chain/height",
				"/push/transaction",
				"/node/info",
				"/node/extended-info",
				"/account/get",
				"/account/status"
		};
		Assert.assertThat(config.getNonAuditedApiPaths(), IsEqual.equalTo(expectedNonAuditedApiPaths));

		Assert.assertThat(config.getNetworkName(), IsEqual.equalTo("mainnet"));
		Assert.assertThat(config.getNetworkInfo(), IsEqual.equalTo(NetworkInfos.getMainNetworkInfo()));
	}

	private static void assertCustomRequiredConfiguration(final CommonConfiguration config) {
		// Assert:
		Assert.assertThat(config.getShortServerName(), IsEqual.equalTo("Ncc"));
		Assert.assertThat(config.getMaxThreads(), IsEqual.equalTo(1));
		Assert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		Assert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		Assert.assertThat(config.getWebContext(), IsEqual.equalTo("/web"));
		Assert.assertThat(config.getApiContext(), IsEqual.equalTo("/api"));
		Assert.assertThat(config.getHomePath(), IsEqual.equalTo("/home"));
	}

	private static void assertCustomOptionalConfiguration(final CommonConfiguration config) {
		// Assert:
		Assert.assertThat(config.getNemFolder(), IsEqual.equalTo("folder"));
		Assert.assertThat(config.getProtocol(), IsEqual.equalTo("ftp"));
		Assert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.1"));
		Assert.assertThat(config.getWebsocketPort(), IsEqual.equalTo(102));
		Assert.assertThat(config.getShutdownPath(), IsEqual.equalTo("/sd"));
		Assert.assertThat(config.useDosFilter(), IsEqual.equalTo(true));
		Assert.assertThat(
				config.getNonAuditedApiPaths(),
				IsEqual.equalTo(new String[] { "/status", "/whatever" }));

		Assert.assertThat(config.getNetworkName(), IsEqual.equalTo("testnet"));
		Assert.assertThat(config.getNetworkInfo(), IsEqual.equalTo(NetworkInfos.getTestNetworkInfo()));
	}

	//endregion

	//region property required status

	@Test
	public void requiredPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new CommonConfiguration(properties);

		// Assert:
		Assert.assertThat(properties.getRequiredPropertyNames(), IsEquivalent.equivalentTo(REQUIRED_PROPERTY_NAMES));
	}

	@Test
	public void optionalPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new CommonConfiguration(properties);

		// Assert:
		Assert.assertThat(properties.getOptionalPropertyNames(), IsEquivalent.equivalentTo(OPTIONAL_PROPERTY_NAMES));
	}

	//endregion

	//region derivative information

	@Test
	public void additionalInformationCanBeRetrieved() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert:
		Assert.assertThat(config.isNcc(), IsEqual.equalTo(true));
		Assert.assertThat(config.getBaseUrl(), IsEqual.equalTo("ftp://10.0.0.1:100"));
		Assert.assertThat(config.getShutdownUrl(), IsEqual.equalTo("ftp://10.0.0.1:100/api/sd"));
		Assert.assertThat(config.getHomeUrl(), IsEqual.equalTo("ftp://10.0.0.1:100/web/home"));
	}

	@Test
	public void canReadEndpointSettingsWithHttpPort() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nem.protocol", "http");
		properties.setProperty("nem.host", "10.0.0.12");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getProtocol(), IsEqual.equalTo("http"));
		Assert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.12"));
		Assert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		Assert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		Assert.assertThat(config.getPort(), IsEqual.equalTo(100));
		Assert.assertThat(config.getBaseUrl(), IsEqual.equalTo("http://10.0.0.12:100"));
		Assert.assertThat(config.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("http", "10.0.0.12", 100)));
	}

	@Test
	public void canReadEndpointSettingsWithHttpsPort() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nem.protocol", "https");
		properties.setProperty("nem.host", "10.0.0.12");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");
		properties.setProperty("nem.websocketPort", "102");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getProtocol(), IsEqual.equalTo("https"));
		Assert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.12"));
		Assert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		Assert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		Assert.assertThat(config.getWebsocketPort(), IsEqual.equalTo(102));
		Assert.assertThat(config.getPort(), IsEqual.equalTo(101));
		Assert.assertThat(config.getBaseUrl(), IsEqual.equalTo("https://10.0.0.12:101"));
		Assert.assertThat(config.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("https", "10.0.0.12", 101)));
	}

	//endregion

	//region specific property tests

	@Test
	public void networkCannotBeParsedWithInvalidValue() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nem.network", "nxt");

		// Act:
		ExceptionAssert.assertThrows(
				v -> new CommonConfiguration(properties),
				IllegalArgumentException.class);
	}

	//endregion

	private static Properties getCommonProperties() {
		final Properties properties = new Properties();
		properties.setProperty("nem.shortServerName", "Ncc");
		properties.setProperty("nem.folder", "folder");
		properties.setProperty("nem.maxThreads", "1");
		properties.setProperty("nem.protocol", "ftp");
		properties.setProperty("nem.host", "10.0.0.1");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");
		properties.setProperty("nem.websocketPort", "102");
		properties.setProperty("nem.webContext", "/web");
		properties.setProperty("nem.apiContext", "/api");
		properties.setProperty("nem.homePath", "/home");
		properties.setProperty("nem.shutdownPath", "/sd");
		properties.setProperty("nem.useDosFilter", "true");
		properties.setProperty("nem.nonAuditedApiPaths", "/status|/whatever");
		properties.setProperty("nem.network", "testnet");
		return properties;
	}
}
