package org.nem.deploy;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;

import java.util.Properties;

public class NisConfigurationTest {

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
		Assert.assertThat(config.getNodeLimit(), IsNull.nullValue());
	}
}