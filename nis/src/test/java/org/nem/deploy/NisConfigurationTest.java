package org.nem.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;

import java.util.Properties;

public class NisConfigurationTest {

	//region nis.bootKey

	@Test
	public void canReadConfigurationWithAutoBootKey() {
		// Arrange:
		final PrivateKey originalPrivateKey = new KeyPair().getPrivateKey();
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootKey", originalPrivateKey.toString());

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
	}

	@Test
	public void canReadConfigurationWithoutAutoBootKey() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsNull.nullValue());
	}

	//endregion

	//region nis.bootName

	@Test
	public void canReadConfigurationWithAutoBootName() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootName", "my name");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("my name"));
	}

	@Test
	public void autoBootNameIsTrimmedOfLeadingAndTrailingWhitespace() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootName", " \t string with spaces\t  ");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("string with spaces"));
	}

	@Test
	public void canReadConfigurationWithoutAutoBootName() {
		// Arrange:
		final Properties properties =getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootName(), IsNull.nullValue());
	}

	//endregion

	//region nis.nodeLimit

	@Test
	public void canReadConfigurationWithNodeLimit() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.nodeLimit", "12");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(12));
	}

	@Test
	public void canReadConfigurationWithoutNodeLimit() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(20));
	}

	//endregion

	//region nis.bootWithoutAck

	@Test
	public void canReadConfigurationWithBootWithoutAck() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootWithoutAck", "true");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.bootWithoutAck(), IsEqual.equalTo(true));
	}

	@Test
	public void canReadConfigurationWithoutBootWithoutAck() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.bootWithoutAck(), IsEqual.equalTo(false));
	}

	//endregion

	//region nis.useBinaryTransport

	@Test
	public void canReadConfigurationWithUseBinaryTransport() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.useBinaryTransport", "true");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(true));
	}

	@Test
	public void canReadConfigurationWithoutUseBinaryTransport() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(false));
	}

	//endregion

	private Properties getCommonProperties() {
		final Properties properties = new Properties();
		properties.setProperty("nem.shortServerName", "Ncc");
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
		properties.setProperty("nem.isWebStart", "false");
		properties.setProperty("nem.nisJnlpUrl", "url");

		return properties;
	}
}