package org.nem.core.deploy;

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
	public Class getAppConfigClass();

	/**
	 * Gets the web application initializer class used by NIS/NCC.
	 *
	 * @return The web application initializer class.
	 */
	public Class getWebAppInitializerClass();

	/**
	 * Gets the jar file class used by NCC.
	 *
	 * @return The jar file class.
	 */
	public Class<? extends HttpServlet> getJarFileServletClass();

	/**
	 * Gets the default servlet class used by NCC.
	 *
	 * @return The default servlet class.
	 */
	public Class<? extends HttpServlet> getDefaultServletClass();

	/**
	 * Starts the default browser if needed and navigates to the supplied url.
	 *
	 * @return True if the browser opened the page, false otherwise.
	 */
	public boolean openWebBrowser(final String homeUrl);

	/**
	 * Starts the NIS server via web start.
	 */
	public void startNISViaWebStart(final String nisJnlpUrl);
}
