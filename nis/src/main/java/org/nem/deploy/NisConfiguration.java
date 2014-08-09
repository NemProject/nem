package org.nem.deploy;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.utils.ExceptionUtils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Class responsible for holding all NIS configuration settings.
 * A NIS reboot is required for configuration changes to take effect.
 *
 * TODO: does it make more sense to expose functions like readIntegerOrDefault, readBooleanOrDefault ... ?
 */
public class NisConfiguration {
	private final int nodeLimit;
	private final PrivateKey bootKey;
	private final boolean shouldBootWithoutAck;
	private final boolean shouldUseBinaryTransport;
	private final String nemFolder;
	private final String bootName;

	/**
	 * Creates a new configuration object from the default properties.
	 */
	public NisConfiguration() {
		this(loadDefaultProperties());
	}

	private static Properties loadDefaultProperties() {
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
	public NisConfiguration(final Properties properties) {
		final String autoBootKey = properties.getProperty("nis.bootkey");
		this.bootKey = null == autoBootKey ? null : PrivateKey.fromHexString(autoBootKey);
		final String autoBootName = properties.getProperty("nis.bootname");
		this.bootName = null == autoBootName ? null : autoBootName.trim();

		this.nodeLimit = getOptionalInteger(properties, "nis.nodelimit", 20);
		this.shouldBootWithoutAck = getOptionalBoolean(properties, "nis.shouldBootWithoutAck", false);
		this.shouldUseBinaryTransport = getOptionalBoolean(properties, "nis.shouldUseBinaryTransport", false);
		this.nemFolder = properties.getProperty("nem.folder", getDefaultFolder()).replace("%h", getDefaultFolder());
	}

	private static int getOptionalInteger(final Properties properties, final String name, final Integer defaultValue) {
		final String value = properties.getProperty(name);
		return null == value ? defaultValue : Integer.valueOf(value);
	}

	private static boolean getOptionalBoolean(final Properties properties, final String name, final Boolean defaultValue) {
		final String value = properties.getProperty(name);
		return null == value ? defaultValue : Boolean.valueOf(value);
	}

	/**
	 * Gets the number of regular nodes that this node should communicate with during broadcasts.
	 *
	 * @return The number of regular nodes that this node should communicate with during broadcasts
	 */
	public int getNodeLimit() {
		return this.nodeLimit;
	}

	/**
	 * Gets the private key of the auto-boot node.
	 * If null, the node shouldn't auto-boot.
	 *
	 * @return The private key of the auto-boot node.
	 */
	public PrivateKey getAutoBootKey() {
		return this.bootKey;
	}

	/**
	 * Gets the name of the auto-boot node.
	 *
	 * @return The name of the auto-boot node.
	 */
	public String getAutoBootName() {
		return this.bootName;
	}

	/**
	 * Gets a value indicating whether or not this node should be allowed to boot if it
	 * cannot receive acknowledgement from a remote peer during the boot sequence.
	 *
	 * @return true if remote peer acknowledgement is optional.
	 */
	public boolean shouldBootWithoutAck() {
		return this.shouldBootWithoutAck;
	}

	/**
	 * Gets a value indicating whether or not this node should send binary payloads.
	 *
	 * @return true if this node should send binary payloads.
	 */
	public boolean shouldUseBinaryTransport() {
		return this.shouldUseBinaryTransport;
	}
	
	/**
	 * Gets the path to the folder where database and log files should be located
	 *
	 * @return The path to the folder
	 */
	public String getNemFolder() {
		return this.nemFolder;
	}


	/**
	 * Get the default folder for database and log files.
	 *  
	 * @return path to the folder location.
	 */
	private String getDefaultFolder() {
		return System.getProperty("user.home");
	}
}
