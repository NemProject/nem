package org.nem.deploy;

import javax.servlet.http.HttpServlet;

/**
 * Interface which supplies additional configuration information.
 */
public interface NemConfigurationPolicy {
	/**
	 * Gets the application configuration class used by NIS/NCC.
	 *
	 * @return The application configuration class.
	 */
	Class getAppConfigClass();

	/**
	 * Gets the web application initializer class used by NIS/NCC.
	 *
	 * @return The web application initializer class.
	 */
	Class getWebAppInitializerClass();

	/**
	 * Gets the jar file class used by NCC.
	 *
	 * @return The jar file class.
	 */
	Class<? extends HttpServlet> getJarFileServletClass();

	/**
	 * Gets the default servlet class used by NCC.
	 *
	 * @return The default servlet class.
	 */
	Class<? extends HttpServlet> getDefaultServletClass();

	/**
	 * Loads the common configuration and replaces default with values
	 * supplied in args if available.
	 *
	 * @param args The optional array of string parameters.
	 * @return The common configuration.
	 */
	CommonConfiguration loadConfig(final String[] args);
}
