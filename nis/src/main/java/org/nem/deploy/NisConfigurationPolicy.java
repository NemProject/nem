package org.nem.deploy;

import org.nem.core.deploy.*;
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
