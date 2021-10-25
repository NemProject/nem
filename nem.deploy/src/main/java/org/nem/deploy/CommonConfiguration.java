package org.nem.deploy;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.node.NodeEndpoint;

import java.util.*;

/**
 * Class responsible for holding all common configuration settings.
 * A reboot is required for configuration changes to take effect.
 */
public class CommonConfiguration {
	private final String shortServerName;
	private final String nemFolder;
	private final int maxThreads;
	private final String protocol;
	private final String host;
	private final int httpPort;
	private final int httpsPort;
	private final int websocketPort;
	private final String webContext;
	private final String apiContext;
	private final String home;
	private final String shutdown;
	private final Boolean useDosFilter;
	private final String[] nonAuditedApiPaths;
	private final String networkName;
	private final NetworkInfo networkInfo;

	/**
	 * Creates a new configuration object from the default properties.
	 */
	public CommonConfiguration() {
		this(loadDefaultProperties());
	}

	protected static Properties loadDefaultProperties() {
		final Class<?> clazz = CommonConfiguration.class;
		final Collection<Properties> propertyBags = Arrays.asList(
				PropertiesExtensions.loadFromResource(clazz, "config.properties", true),
				PropertiesExtensions.loadFromResource(clazz, "config-user.properties", false));
		return PropertiesExtensions.merge(propertyBags);
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param properties The specified properties.
	 */
	public CommonConfiguration(final Properties properties) {
		this(new NemProperties(properties));
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param properties The specified properties.
	 */
	public CommonConfiguration(final NemProperties properties) {
		this.shortServerName = properties.getString("nem.shortServerName");

		// use '/' as the path separator in the default value in order to match the value in the resources file
		// otherwise, the default value (from resources) and the default value (in code) will not match on all OSs
		this.nemFolder = properties.getOptionalString("nem.folder", "%h/nem")
				.replace("/", System.getProperty("file.separator"))
				.replace("%h", this.getDefaultFolder());

		this.maxThreads = properties.getInteger("nem.maxThreads");
		this.protocol = properties.getOptionalString("nem.protocol", "http");
		this.host = properties.getOptionalString("nem.host", "127.0.0.1");
		this.httpPort = properties.getInteger("nem.httpPort");
		this.httpsPort = properties.getInteger("nem.httpsPort");
		this.websocketPort = properties.getOptionalInteger("nem.websocketPort", 7778);
		this.webContext = properties.getString("nem.webContext");
		this.apiContext = properties.getString("nem.apiContext");
		this.home = properties.getString("nem.homePath");
		this.shutdown = properties.getOptionalString("nem.shutdownPath", "/shutdown");
		this.useDosFilter = properties.getOptionalBoolean("nem.useDosFilter", true);
		this.nonAuditedApiPaths = properties.getOptionalStringArray(
				"nem.nonAuditedApiPaths",
				"/heartbeat|/status|/chain/height|/push/transaction|/node/info|/node/extended-info|/account/get|/account/status");

		this.networkName = properties.getOptionalString("nem.network", "mainnet");

		if (NetworkInfos.isKnownNetworkFriendlyName(this.networkName)) {
			this.networkInfo = NetworkInfos.fromFriendlyName(this.networkName);
		} else {
			this.networkInfo = new NetworkInfo(
					(byte)properties.getInteger("nem.network.version"),
					properties.getString("nem.network.addressStartChar").charAt(0),
					new NemesisBlockInfo(
							Hash.fromHexString(properties.getString("nem.network.generationHash")),
							Address.fromEncoded(properties.getString("nem.network.nemesisSignerAddress")),
							Amount.fromNem(properties.getLong("nem.network.totalAmount")),
							properties.getString("nem.network.nemesisFilePath")));
		}
	}

	//region basic settings

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
	 * Gets a value indicating if the underlying server is the NCC server.
	 *
	 * @return True if the server is NCC, false otherwise.
	 */
	public boolean isNcc() {
		return this.shortServerName.toUpperCase().equals("NCC");
	}

	/**
	 * Gets the maximum number of threads used for the thread pool.
	 *
	 * @return The maximum number of threads.
	 */
	public int getMaxThreads() {
		return this.maxThreads;
	}

	//endregion

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
	 * Gets the port used for websocket communication layer.
	 *
	 * @return The port.
	 */
	public int getWebsocketPort() {
		return websocketPort;
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

	//region web servlet settings

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

	//endregion

	//region audit settings

	/**
	 * Gets the APIs that shouldn't be audited.
	 *
	 * @return The APIs that shouldn't be audited.
	 */
	public String[] getNonAuditedApiPaths() {
		return this.nonAuditedApiPaths;
	}

	//endregion

	//region network settings

	/**
	 * Gets the network name.
	 *
	 * @return The network name.
	 */
	public String getNetworkName() {
		return this.networkName;
	}

	/**
	 * Gets the network information.
	 *
	 * @return The network information.
	 */
	public NetworkInfo getNetworkInfo() {
		return this.networkInfo;
	}

	//endregion
}
