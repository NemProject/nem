package org.nem.core.deploy;

import org.nem.core.node.NodeEndpoint;
import org.nem.core.utils.ExceptionUtils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Class responsible for holding all common configuration settings.
 * A reboot is required for configuration changes to take effect.
 */
public class CommonConfiguration {
	private final String shortServerName;
	private String nemFolder;
	private final int maxThreads;
	private final String protocol;
	private final String host;
	private final int httpPort;
	private final int httpsPort;
	private final String webContext;
	private final String apiContext;
	private final String home;
	private final String shutdown;
	private final Boolean useDosFilter;

	/**
	 * Creates a new configuration object from the default properties.
	 */
	public CommonConfiguration() {
		this(loadDefaultProperties());
	}

	protected static Properties loadDefaultProperties() {
		return ExceptionUtils.propagate(() -> {
			try (final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("config.properties")) {
				final Properties properties = new Properties();
				properties.load(inputStream);
				return properties;
			}
		}, IllegalStateException::new);
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param properties The specified properties.
	 */
	public CommonConfiguration(final Properties properties) {
		this.shortServerName = getString(properties, "nem.shortServerName");
		this.nemFolder = getOptionalString(properties, "nem.folder", this.getDefaultFolder()).replace("%h", this.getDefaultFolder());
		this.maxThreads = getInteger(properties, "nem.maxThreads");
		this.protocol = getOptionalString(properties, "nem.protocol", "http");
		this.host = getOptionalString(properties, "nem.host", "localhost");
		this.httpPort = getInteger(properties, "nem.httpPort");
		this.httpsPort = getInteger(properties, "nem.httpsPort");
		this.webContext = getString(properties, "nem.webContext");
		this.apiContext = getString(properties, "nem.apiContext");
		this.home = getString(properties, "nem.homePath");
		this.shutdown = getOptionalString(properties, "nem.shutdownPath", "/shutdown");
		this.useDosFilter = getOptionalBoolean(properties, "nem.useDosFilter", false);
	}

	protected static String getString(final Properties properties, final String name) {
		final String value = properties.getProperty(name);
		if (null == value) {
			throw new RuntimeException(String.format("property %s must not be null", name));
		}
		return value;
	}

	protected static int getInteger(final Properties properties, final String name) {
		final String value = properties.getProperty(name);
		if (null == value) {
			throw new RuntimeException(String.format("property %s must not be null", name));
		}
		return Integer.valueOf(properties.getProperty(name));
	}

	protected static String getOptionalString(final Properties properties, final String name, final String defaultValue) {
		final String value = properties.getProperty(name);
		return null == value ? defaultValue : value;
	}

	protected static int getOptionalInteger(final Properties properties, final String name, final Integer defaultValue) {
		final String value = properties.getProperty(name);
		return null == value ? defaultValue : Integer.valueOf(value);
	}

	protected static boolean getOptionalBoolean(final Properties properties, final String name, final Boolean defaultValue) {
		final String value = properties.getProperty(name);
		return null == value ? defaultValue : Boolean.valueOf(value);
	}

	/**
	 * Get the default folder for database and log files.
	 *
	 * @return path to the folder location.
	 */
	private String getDefaultFolder() {
		return System.getProperty("user.home");
	}

	/**
	 * Gets the short name for the server.
	 *
	 * @return The short server name.
	 */
	public String getShortServerName() {
		return this.shortServerName;
	}

	/**
	 * Gets the path to the folder where database and log files should be located.
	 *
	 * @return The path to the folder.
	 */
	public String getNemFolder() {
		return this.nemFolder;
	}

	/**
	 * Gets the maximum number of threads used for the thread pool.
	 *
	 * @return The maximum number of threads.
	 */
	public int getMaxThreads() {
		return this.maxThreads;
	}

	//region endpoint settings

	/**
	 * Gets the protocol used for communication.
	 *
	 * @return The protocol used.
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Gets the host address.
	 *
	 * @return The host address.
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Gets the port used for http protocol communication.
	 *
	 * @return The port.
	 */
	public int getHttpPort() {
		return this.httpPort;
	}

	/**
	 * Gets the port used for https protocol communication.
	 *
	 * @return The port.
	 */
	public int getHttpsPort() {
		return this.httpsPort;
	}

	/**
	 * Gets the port used for communication.
	 *
	 * @return The port.
	 */
	public int getPort() {
		return this.getProtocol().equals("https") ? this.getHttpsPort() : this.getHttpPort();
	}

	/**
	 * Gets the base url as a string.
	 *
	 * @return The base url as string.
	 */
	public String getBaseUrl() {
		return String.format("%s://%s:%d",
				this.getProtocol(),
				this.getHost(),
				this.getPort());
	}

	/**
	 * Gets the endpoint.
	 *
	 * @return The endpoint.
	 */
	public NodeEndpoint getEndpoint() {
		return new NodeEndpoint(this.getProtocol(), this.getHost(), this.getPort());
	}

	//endregion

	/**
	 * Gets the base path for web site paths.
	 *
	 * @return The web context path.
	 */
	public String getWebContext() {
		return this.webContext;
	}

	/**
	 * Gets the base path for API requests.
	 *
	 * @return The api context path.
	 */
	public String getApiContext() {
		return this.apiContext;
	}

	/**
	 * Gets the home path.
	 *
	 * @return The home path.
	 */
	public String getHomePath() {
		return this.home;
	}

	/**
	 * Gets the shutdown path.
	 *
	 * @return The shutdown path.
	 */
	public String getShutdownPath() {
		return this.shutdown;
	}

	/**
	 * Get a value indicating whether or not a DOS filter should be used.
	 *
	 * @return true if a DOS filter should be used, false otherwise.
	 */
	public boolean useDosFilter() {
		return this.useDosFilter;
	}

	/**
	 * .Gets a value indicating if the underlying server is the NCC server.
	 *
	 * @return True if the server is NCC, false otherwise.
	 */
	public boolean isNcc() {
		return this.shortServerName.toUpperCase().equals("NCC");
	}

	/**
	 * Get the shutdown url as string.
	 *
	 * @return The shutdown url as string.
	 */
	public String getShutdownUrl() {
		return String.format("%s%s%s",
				this.getBaseUrl(),
				this.getApiContext(),
				this.getShutdownPath());
	}

	/**
	 * Get the home url as string.
	 *
	 * @return The home url as string.
	 */
	public String getHomeUrl() {
		return String.format("%s%s%s",
				this.getBaseUrl(),
				this.getWebContext(),
				this.getHomePath());
	}
}
