package org.nem.core.deploy;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

import java.util.Properties;

public class CommonConfigurationTest {

	//region valid construction/retrieval of information

	@Test
	public void allPropertiesAreSetInConstructor() {
		// Arrange:
		final Properties properties = this.getCommonProperties();

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert
		Assert.assertThat(config.getShortServerName(), IsEqual.equalTo("Ncc"));
		Assert.assertThat(config.getNemFolder(), IsEqual.equalTo("folder"));
		Assert.assertThat(config.getMaxThreads(), IsEqual.equalTo(1));
		Assert.assertThat(config.getProtocol(), IsEqual.equalTo("ftp"));
		Assert.assertThat(config.getHost(), IsEqual.equalTo("10.0.0.1"));
		Assert.assertThat(config.getHttpPort(), IsEqual.equalTo(100));
		Assert.assertThat(config.getHttpsPort(), IsEqual.equalTo(101));
		Assert.assertThat(config.getWebContext(), IsEqual.equalTo("/web"));
		Assert.assertThat(config.getApiContext(), IsEqual.equalTo("/api"));
		Assert.assertThat(config.getHomePath(), IsEqual.equalTo("/home"));
		Assert.assertThat(config.getShutdownPath(), IsEqual.equalTo("/sd"));
		Assert.assertThat(config.useDosFilter(), IsEqual.equalTo(true));
		Assert.assertThat(config.isWebStart(), IsEqual.equalTo(true));
		Assert.assertThat(config.getNisJnlpUrl(), IsEqual.equalTo("url"));
	}

	@Test
	public void additionalInformationCanBeRetrieved() {
		// Arrange:
		final Properties properties = this.getCommonProperties();

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		// Assert
		Assert.assertThat(config.isNcc(), IsEqual.equalTo(true));
		Assert.assertThat(config.getBaseUrl(), IsEqual.equalTo("ftp://10.0.0.1:100"));
		Assert.assertThat(config.getShutdownUrl(), IsEqual.equalTo("ftp://10.0.0.1:100/api/sd"));
		Assert.assertThat(config.getHomeUrl(), IsEqual.equalTo("ftp://10.0.0.1:100/web/home"));
	}

	//endregion

	//region mandatory entries

	@Test(expected = RuntimeException.class)
	public void cannotReadConfigurationWithoutShortServerName() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.shortServerName");

		// Act:
		new CommonConfiguration(properties);
	}

	@Test
	public void cannotReadConfigurationWithoutMaxThreads() {
		// Act:
		this.assertIntPropertyIsRequired("nem.maxThreads");
	}

	@Test
	public void cannotReadConfigurationWithUnparsableMaxThreads() {
		// Act:
		this.assertIntPropertyMustBeParsable("nem.maxThreads");
	}

	@Test
	public void cannotReadConfigurationWithoutHttpPort() {
		// Act:
		this.assertIntPropertyIsRequired("nem.httpPort");
	}

	@Test
	public void cannotReadConfigurationWithUnparsableHttpPort() {
		// Act:
		this.assertIntPropertyMustBeParsable("nem.httpPort");
	}

	@Test
	public void cannotReadConfigurationWithoutHttpsPort() {
		// Act:
		this.assertIntPropertyIsRequired("nem.httpsPort");
	}

	@Test
	public void cannotReadConfigurationWithUnparsableHttpsPort() {
		// Act:
		this.assertIntPropertyMustBeParsable("nem.httpsPort");
	}

	@Test(expected = RuntimeException.class)
	public void cannotReadConfigurationWithoutWebContext() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.webContext");

		// Act:
		new CommonConfiguration(properties);
	}

	@Test(expected = RuntimeException.class)
	public void cannotReadConfigurationWithoutApiContext() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.apiContext");

		// Act:
		new CommonConfiguration(properties);
	}

	@Test(expected = RuntimeException.class)
	public void cannotReadConfigurationWithoutHomePath() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.homePath");

		// Act:
		new CommonConfiguration(properties);
	}

	//endregion

	//region optional entries

	@Test
	public void canReadConfigurationWithoutFolder() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.folder");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.getNemFolder(), IsEqual.equalTo(System.getProperty("user.home")));
	}

	@Test
	public void canReadConfigurationWithoutProtocol() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.protocol");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.getProtocol(), IsEqual.equalTo("http"));
	}

	@Test
	public void canReadConfigurationWithoutHost() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.host");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.getHost(), IsEqual.equalTo("localhost"));
	}

	@Test
	public void canReadConfigurationWithoutShutdownPath() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.shutdownPath");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.getShutdownPath(), IsEqual.equalTo("/shutdown"));
	}

	@Test
	public void canReadConfigurationWithoutUseDosFilter() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.useDosFilter");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.useDosFilter(), IsEqual.equalTo(false));
	}

	@Test
	public void canReadConfigurationWithoutIsWebStart() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.isWebStart");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.isWebStart(), IsEqual.equalTo(false));
	}

	@Test
	public void canReadConfigurationWithoutNisJnlpUrl() {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove("nem.nisJnlpUrl");

		// Act:
		final CommonConfiguration config = new CommonConfiguration(properties);

		Assert.assertThat(config.getNisJnlpUrl(), IsEqual.equalTo("http://bob.nem.ninja/webstart/nem-server.jnlp"));
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
		properties.setProperty("nem.shutdownPath", "/sd");
		properties.setProperty("nem.useDosFilter", "true");
		properties.setProperty("nem.isWebStart", "true");
		properties.setProperty("nem.nisJnlpUrl", "url");

		return properties;
	}

	private void assertIntPropertyIsRequired(final String propName) {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.remove(propName);

		// Act:
		ExceptionAssert.assertThrows(v -> new CommonConfiguration(properties), RuntimeException.class);
	}

	private void assertIntPropertyMustBeParsable(final String propName) {
		// Arrange:
		final Properties properties = this.getCommonProperties();
		properties.setProperty(propName, "notANumber");

		// Act:
		ExceptionAssert.assertThrows(v -> new CommonConfiguration(properties), NumberFormatException.class);
	}
}
