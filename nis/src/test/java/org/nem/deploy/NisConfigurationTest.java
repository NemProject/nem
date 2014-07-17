package org.nem.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;

import java.util.Properties;

public class NisConfigurationTest {

	//region nis.bootkey

	@Test
	public void canReadConfigurationWithAutoBootKey() {
		// Arrange:
		final PrivateKey originalPrivateKey = new KeyPair().getPrivateKey();
		final Properties properties = new Properties();
		properties.setProperty("nis.bootkey", originalPrivateKey.toString());

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
		Assert.assertThat(config.getAutoBootName(), IsNull.nullValue());
	}

	@Test
	public void canReadConfigurationWithAutoBootKeyAndName() {
		// Arrange:
		final PrivateKey originalPrivateKey = new KeyPair().getPrivateKey();
		final Properties properties = new Properties();
		properties.setProperty("nis.bootkey", originalPrivateKey.toString());
		properties.setProperty("nis.bootname", " \t string with spaces  ");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
		Assert.assertThat(config.getAutoBootName(), IsEqual.equalTo("string with spaces"));
	}

	@Test
	public void canReadConfigurationWithoutAutoBootKey() {
		// Arrange:
		final Properties properties = new Properties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getAutoBootKey(), IsNull.nullValue());
	}

	//endregion

	//region nis.nodelimit

	@Test
	public void canReadConfigurationWithNodeLimit() {
		// Arrange:
		final Properties properties = new Properties();
		properties.setProperty("nis.nodelimit", "12");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(12));
	}

	@Test
	public void canReadConfigurationWithoutNodeLimit() {
		// Arrange:
		final Properties properties = new Properties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.getNodeLimit(), IsEqual.equalTo(20));
	}

	//endregion

	//region nis.shouldBootWithoutAck

	@Test
	public void canReadConfigurationWithShouldBootWithoutAck() {
		// Arrange:
		final Properties properties = new Properties();
		properties.setProperty("nis.shouldBootWithoutAck", "true");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.shouldBootWithoutAck(), IsEqual.equalTo(true));
	}

	@Test
	public void canReadConfigurationWithoutShouldBootWithoutAck() {
		// Arrange:
		final Properties properties = new Properties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.shouldBootWithoutAck(), IsEqual.equalTo(false));
	}

	//endregion

	//region nis.shouldUseBinaryTransport

	@Test
	public void canReadConfigurationWithShouldUseBinaryTransport() {
		// Arrange:
		final Properties properties = new Properties();
		properties.setProperty("nis.shouldUseBinaryTransport", "true");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.shouldUseBinaryTransport(), IsEqual.equalTo(true));
	}

	@Test
	public void canReadConfigurationWithoutShouldUseBinaryTransport() {
		// Arrange:
		final Properties properties = new Properties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		Assert.assertThat(config.shouldUseBinaryTransport(), IsEqual.equalTo(false));
	}

	//endregion
}