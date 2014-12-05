package org.nem.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;

import java.util.Properties;

public class NisConfigurationTest {

	@Test
	public void canReadDefaultConfiguration() {
		// Arrange:
		final Properties properties = this.getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		assertDefaultConfiguration(config);
	}

	@Test
	public void canReadDefaultConfigurationFromResources() {
		// Act:
		final NisConfiguration config = new NisConfiguration();

		// Assert:
		assertDefaultConfiguration(config);
	}

	private static void assertDefaultConfiguration(final NisConfiguration config) {
		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsNull.nullValue());
		Assert.assertThat(config.getAutoBootName(), IsNull.nullValue());
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(5));
		Assert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(20));
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(true));
		Assert.assertThat(config.useNetworkTime(), IsEqual.equalTo(true));
		Assert.assertThat(config.getIpDetectionMode(), IsEqual.equalTo(IpDetectionMode.AutoRequired));
		Assert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(1));
		Assert.assertThat(
				config.getNonAuditedApiPaths(),
				IsEqual.equalTo(new String[] { "/heartbeat", "/status", "/chain/height", "/push/transaction", "/node/info", "/node/extended-info" }));
		Assert.assertThat(config.getMaxTransactions(), IsEqual.equalTo(10000));
		Assert.assertThat(config.getTransactionHashRetentionTime(), IsEqual.equalTo(36));
		Assert.assertThat(
				config.getAdditionalLocalIps(),
				IsEqual.equalTo(new String[] { }));
	}

	@Test
	public void canReadCustomConfiguration() {
		// Arrange:
		final PrivateKey originalPrivateKey = new KeyPair().getPrivateKey();
		final Properties properties = this.getCommonProperties();
		properties.setProperty("nis.bootKey", originalPrivateKey.toString());
		properties.setProperty("nis.bootName", "my name");
		properties.setProperty("nis.nodeLimit", "8");
		properties.setProperty("nis.timeSyncNodeLimit", "12");
		properties.setProperty("nis.useBinaryTransport", "false");
		properties.setProperty("nis.useNetworkTime", "false");
		properties.setProperty("nis.ipDetectionMode", "Disabled");
		properties.setProperty("nis.unlockedLimit", "123");
		properties.setProperty("nis.nonAuditedApiPaths", "/status|/whatever");
		properties.setProperty("nis.maxTransactions", "234");
		properties.setProperty("nis.transactionHashRetentionTime", "567");
		properties.setProperty("nis.additionalLocalIps", "10.0.0.10|10.0.0.20");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("my name"));
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(8));
		Assert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(12));
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(false));
		Assert.assertThat(config.useNetworkTime(), IsEqual.equalTo(false));
		Assert.assertThat(config.getIpDetectionMode(), IsEqual.equalTo(IpDetectionMode.Disabled));
		Assert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(123));
		Assert.assertThat(
				config.getNonAuditedApiPaths(),
				IsEqual.equalTo(new String[] { "/status", "/whatever" }));
		Assert.assertThat(config.getMaxTransactions(), IsEqual.equalTo(234));
		Assert.assertThat(config.getTransactionHashRetentionTime(), IsEqual.equalTo(567));
		Assert.assertThat(
				config.getAdditionalLocalIps(),
				IsEqual.equalTo(new String[] { "10.0.0.10", "10.0.0.20" }));
	}

	@Test
	public void autoBootNameIsTrimmedOfLeadingAndTrailingWhitespace() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.setProperty("nis.bootName", " \t string with spaces\t  ");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("string with spaces"));
	}

	//region canReadStringArray

	@Test
	public void canReadStringArrayWithNoValues() {
		// Assert:
		this.assertCanReadStringArray(" \t \t", new String[] { });
	}

	@Test
	public void canReadStringArrayWithSingleValue() {
		// Assert:
		this.assertCanReadStringArray("10.0.0.10", new String[] { "10.0.0.10" });
	}

	@Test
	public void canReadStringArrayWithMultipleValues() {
		// Assert:
		this.assertCanReadStringArray(
				"10.0.0.10|10.0.0.20|10.0.0.30",
				new String[] { "10.0.0.10", "10.0.0.20", "10.0.0.30" });
	}

	@Test
	public void canReadStringArrayWithBlankValues() {
		// Assert:
		this.assertCanReadStringArray(
				"10.0.0.10|| |10.0.0.30",
				new String[] { "10.0.0.10", "", " ", "10.0.0.30" });
	}

	private void assertCanReadStringArray(final String value, final String[] expectedValues) {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.setProperty("nis.additionalLocalIps", value);

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAdditionalLocalIps(), IsEqual.equalTo(expectedValues));
	}

	//endregion

	private Properties getCommonProperties() {
		final Properties properties = new Properties();
		properties.setProperty("nem.shortServerName", "Nis");
		properties.setProperty("nem.folder", "folder");
		properties.setProperty("nem.maxThreads", "1");
		properties.setProperty("nem.protocol", "ftp");
		properties.setProperty("nem.host", "10.0.0.1");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");
		properties.setProperty("nem.webContext", "/web");
		properties.setProperty("nem.apiContext", "/api");
		properties.setProperty("nem.homePath", "/home");
		properties.setProperty("nem.shutdownPath", "/shutdown");
		properties.setProperty("nem.useDosFilter", "true");
		return properties;
	}
}