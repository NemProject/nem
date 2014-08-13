package org.nem.deploy;

import org.nem.core.deploy.NemConfigurationPolicy;
import org.nem.deploy.appconfig.NisAppConfig;

import javax.servlet.http.HttpServlet;

/**
 * Class for supplying addition NIS configuration information.
 */
public class NisConfigurationPolicy implements NemConfigurationPolicy {
	@Override
	public Class getAppConfigClass() {
		return NisAppConfig.class;
	}

	@Override
	public Class getWebAppInitializerClass() {
		return NisWebAppInitializer.class;
	}

	@Override
	public Class<? extends HttpServlet> getJarFileServletClass() {
		throw new RuntimeException("getJarFileServletClass is not supposed to be called from NIS server.");
	}

	@Override
	public Class<? extends HttpServlet> getDefaultServletClass() {
		throw new RuntimeException("getDefaultServletClass is not supposed to be called from NIS server.");
	}

	@Override
	public boolean openWebBrowser(final String homeUrl) {
		throw new RuntimeException("openWebBrowser is not supposed to be called from NIS server.");
	}

	@Override
	public void startNisViaWebStart(final String nisJnlpUrl) {
		throw new RuntimeException("startNISViaWebStart is not supposed to be called from NIS server.");
	}
}
