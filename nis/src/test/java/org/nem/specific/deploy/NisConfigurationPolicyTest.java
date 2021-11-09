package org.nem.specific.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.deploy.CommonConfiguration;
import org.nem.specific.deploy.appconfig.NisAppConfig;

public class NisConfigurationPolicyTest {

	// region get class

	@Test
	public void canGetNisAppConfigClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		MatcherAssert.assertThat(policy.getAppConfigClass(), IsEqual.equalTo(NisAppConfig.class));
	}

	@Test
	public void canGetNisWebAppInitializerClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		MatcherAssert.assertThat(policy.getWebAppInitializerClass(), IsEqual.equalTo(NisWebAppInitializer.class));
	}

	@Test
	public void canGetNisWebAppWebsockInitializerClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		MatcherAssert.assertThat(policy.getWebAppWebsockInitializerClass(), IsEqual.equalTo(NisWebAppWebsocketInitializer.class));
	}

	// endregion

	// region get raises exception

	@Test(expected = NisConfigurationException.class)
	public void getJarFileServletClassRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.getJarFileServletClass();
	}

	@Test(expected = NisConfigurationException.class)
	public void getRootServletClassRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.getRootServletClass();
	}

	// endregion

	@Test
	public void loadConfigReturnsValidConfiguration() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		final CommonConfiguration configuration = policy.loadConfig(new String[]{});

		// Assert:
		MatcherAssert.assertThat(configuration, IsNull.notNullValue());
	}
}
