package org.nem.deploy;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.deploy.appconfig.NisAppConfig;

public class NisConfigurationPolicyTest {
	//region get class

	@Test
	public void canGetNisAppConfigClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		Assert.assertThat(policy.getAppConfigClass(), IsEqual.equalTo(NisAppConfig.class));
	}

	@Test
	public void canGetNisWebAppInitializerClass() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Assert:
		Assert.assertThat(policy.getWebAppInitializerClass(), IsEqual.equalTo(NisWebAppInitializer.class));
	}

	//endregion

	//region get raises exception

	@Test(expected = RuntimeException.class)
	public void getJarFileServletClassRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.getJarFileServletClass();
	}

	@Test(expected = RuntimeException.class)
	public void getDefaultServletClassRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.getDefaultServletClass();
	}

	@Test(expected = RuntimeException.class)
	public void openWebBrowserRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.openWebBrowser("http://127.0.0.1:7890//ncc/web/index.html");
	}

	@Test(expected = RuntimeException.class)
	public void startNisViaWebStartRaisesException() {
		// Arrange:
		final NisConfigurationPolicy policy = new NisConfigurationPolicy();

		// Act:
		policy.handleWebStart(null);
	}

	//endregion
}
