package org.nem.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
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

		// Assert:
		assertCustomRequiredConfiguration(config);
		assertCustomOptionalConfiguration(config, true);
	}

	@Test
	public void canReadConfigurationWithCustomNetwork() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nem.network", "foobar");
		properties.setProperty("nem.network.version", "111");
		properties.setProperty("nem.network.addressStartChar", "Z");
		properties.setProperty("nem.network.generationHash", "0D35091344727AD5E175D7CE4DE7A6DAFD08EE135E09774DDCA2C55626264F45");
		properties.setProperty("nem.network.nemesisSignerAddress", "ZAAFEBANNRXGOEIHNIRR5DDYYNN6IEKKO75O64RR");
		properties.setProperty("nem.network.totalAmount", "888888888");
		properties.setProperty("nem.network.nemesisFilePath", "path/to/file.bin");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert:
		assertCustomRequiredConfiguration(config);
		assertCustomOptionalConfiguration(config, false);

		MatcherAssert.assertThat(config.getNetworkName(), IsEqual.equalTo("foobar"));
		MatcherAssert.assertThat(config.getNetworkInfo().getVersion(), IsEqual.equalTo((byte)111));
		MatcherAssert.assertThat(config.getNetworkInfo().getAddressStartChar(), IsEqual.equalTo('Z'));

		final NemesisBlockInfo nemesisBlockInfo = config.getNetworkInfo().getNemesisBlockInfo();
		MatcherAssert.assertThat(
				nemesisBlockInfo.getGenerationHash(),
				IsEqual.equalTo(Hash.fromHexString("0D35091344727AD5E175D7CE4DE7A6DAFD08EE135E09774DDCA2C55626264F45")));
		MatcherAssert.assertThat(nemesisBlockInfo.getAddress(), IsEqual.equalTo(Address.fromEncoded("ZAAFEBANNRXGOEIHNIRR5DDYYNN6IEKKO75O64RR")));
		MatcherAssert.assertThat(nemesisBlockInfo.getAmount(), IsEqual.equalTo(Amount.fromNem(888888888)));
		MatcherAssert.assertThat(nemesisBlockInfo.getDataFileName(), IsEqual.equalTo("path/to/file.bin"));
	}

	@Test
	public void canReadCustomConfigurationWithoutOptionalProperties() {
		// Arrange:
		final Properties properties = getCommonProperties();
		OPTIONAL_PROPERTY_NAMES.forEach(properties::remove);

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert:
		assertCustomRequiredConfiguration(config);
		assertDefaultOptionalConfiguration(config);
	}

	private static void assertDefaultRequiredConfiguration(final CommonConfiguration config) {
		// Assert:
		MatcherAssert.assertThat(config.getShortServerName(), IsEqual.equalTo("Nis"));
		MatcherAssert.assertThat(config.getMaxThreads(), IsEqual.equalTo(500));
		MatcherAssert.assertThat(config.getHttpPort(), IsEqual.equalTo(7890));
		MatcherAssert.assertThat(config.getHttpsPort(), IsEqual.equalTo(7891));
		MatcherAssert.assertThat(config.getWebContext(), IsEqual.equalTo(""));
		MatcherAssert.assertThat(config.getApiContext(), IsEqual.equalTo(""));
		MatcherAssert.assertThat(config.getHomePath(), IsEqual.equalTo(""));
	}

	private static void assertDefaultOptionalConfiguration(final CommonConfiguration config) {
		// Assert:
		MatcherAssert.assertThat(config.getNemFolder(), IsEqual.equalTo(Paths.get(System.getProperty("user.home"), "nem").toString()));
		MatcherAssert.assertThat(config.getProtocol(), IsEqual.equalTo("http"));
		MatcherAssert.assertThat(config.getHost(), IsEqual.equalTo("127.0.0.1"));
		MatcherAssert.assertThat(config.getWebsocketPort(), IsEqual.equalTo(7778));
		MatcherAssert.assertThat(config.getShutdownPath(), IsEqual.equalTo("/shutdown"));
		MatcherAssert.assertThat(config.useDosFilter(), IsEqual.equalTo(true));

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
		MatcherAssert.assertThat(config.getNonAuditedApiPaths(), IsEqual.equalTo(expectedNonAuditedApiPaths));

		MatcherAssert.assertThat(config.getNetworkName(), IsEqual.equalTo("mainnet"));
		MatcherAssert.assertThat(config.getNetworkInfo(), IsEqual.equalTo(NetworkInfos.getMainNetworkInfo()));
	}

	private static void assertCustomRequiredConfiguration(final CommonConfiguration config) {
		// Assert:
		MatcherAssert.assertThat(config.getShortServerName(), IsEqual.equalTo("Ncc"));
		MatcherAssert.assertThat(config.getMaxThreads(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		MatcherAssert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		MatcherAssert.assertThat(config.getWebContext(), IsEqual.equalTo("/web"));
		MatcherAssert.assertThat(config.getApiContext(), IsEqual.equalTo("/api"));
		MatcherAssert.assertThat(config.getHomePath(), IsEqual.equalTo("/home"));
	}

	private static void assertCustomOptionalConfiguration(final CommonConfiguration config, final boolean checkNetwork) {
		// Assert:
		MatcherAssert.assertThat(config.getNemFolder(), IsEqual.equalTo("folder"));
		MatcherAssert.assertThat(config.getProtocol(), IsEqual.equalTo("ftp"));
		MatcherAssert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.1"));
		MatcherAssert.assertThat(config.getWebsocketPort(), IsEqual.equalTo(102));
		MatcherAssert.assertThat(config.getShutdownPath(), IsEqual.equalTo("/sd"));
		MatcherAssert.assertThat(config.useDosFilter(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(
				config.getNonAuditedApiPaths(),
				IsEqual.equalTo(new String[] { "/status", "/whatever" }));

		if (checkNetwork) {
			MatcherAssert.assertThat(config.getNetworkName(), IsEqual.equalTo("testnet"));
			MatcherAssert.assertThat(config.getNetworkInfo(), IsEqual.equalTo(NetworkInfos.getTestNetworkInfo()));
		}
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
		MatcherAssert.assertThat(properties.getRequiredPropertyNames(), IsEquivalent.equivalentTo(REQUIRED_PROPERTY_NAMES));
	}

	@Test
	public void optionalPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new CommonConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(properties.getOptionalPropertyNames(), IsEquivalent.equivalentTo(OPTIONAL_PROPERTY_NAMES));
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
		MatcherAssert.assertThat(config.isNcc(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.getBaseUrl(), IsEqual.equalTo("ftp://10.0.0.1:100"));
		MatcherAssert.assertThat(config.getShutdownUrl(), IsEqual.equalTo("ftp://10.0.0.1:100/api/sd"));
		MatcherAssert.assertThat(config.getHomeUrl(), IsEqual.equalTo("ftp://10.0.0.1:100/web/home"));
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
		MatcherAssert.assertThat(config.getProtocol(), IsEqual.equalTo("http"));
		MatcherAssert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.12"));
		MatcherAssert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		MatcherAssert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		MatcherAssert.assertThat(config.getPort(), IsEqual.equalTo(100));
		MatcherAssert.assertThat(config.getBaseUrl(), IsEqual.equalTo("http://10.0.0.12:100"));
		MatcherAssert.assertThat(config.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("http", "10.0.0.12", 100)));
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
		MatcherAssert.assertThat(config.getProtocol(), IsEqual.equalTo("https"));
		MatcherAssert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.12"));
		MatcherAssert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		MatcherAssert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		MatcherAssert.assertThat(config.getWebsocketPort(), IsEqual.equalTo(102));
		MatcherAssert.assertThat(config.getPort(), IsEqual.equalTo(101));
		MatcherAssert.assertThat(config.getBaseUrl(), IsEqual.equalTo("https://10.0.0.12:101"));
		MatcherAssert.assertThat(config.getEndpoint(), IsEqual.equalTo(new NodeEndpoint("https", "10.0.0.12", 101)));
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
