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
		Assert.assertThat(config.getAutoBootKey(), IsNull.nullValue());
		Assert.assertThat(config.getAutoBootName(), IsNull.nullValue());
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(5));
		Assert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(20));
		Assert.assertThat(config.bootWithoutAck(), IsEqual.equalTo(false));
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(true));
		Assert.assertThat(config.useNetworkTime(), IsEqual.equalTo(true));
		Assert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(1));
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
		properties.setProperty("nis.bootWithoutAck", "true");
		properties.setProperty("nis.useBinaryTransport", "false");
		properties.setProperty("nis.useNetworkTime", "false");
		properties.setProperty("nis.unlockedLimit", "123");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("my name"));
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(8));
		Assert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(12));
		Assert.assertThat(config.bootWithoutAck(), IsEqual.equalTo(true));
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(false));
		Assert.assertThat(config.useNetworkTime(), IsEqual.equalTo(false));
		Assert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(123));
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