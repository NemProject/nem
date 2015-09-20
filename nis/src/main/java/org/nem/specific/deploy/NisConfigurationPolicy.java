package org.nem.specific.deploy;

import org.nem.deploy.*;
import org.nem.specific.deploy.appconfig.NisAppConfig;

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
	public Class getWebAppWebsockInitializerClass() {
		return null;
	}

	@Override
	public Class<? extends HttpServlet> getJarFileServletClass() {
		throw new NisConfigurationException("getJarFileServletClass is not supposed to be called from NIS server.");
	}

	@Override
	public Class<? extends HttpServlet> getDefaultServletClass() {
		throw new NisConfigurationException("getDefaultServletClass is not supposed to be called from NIS server.");
	}

	@Override
	public CommonConfiguration loadConfig(final String[] args) {
		return new NisConfiguration();
	}
}
