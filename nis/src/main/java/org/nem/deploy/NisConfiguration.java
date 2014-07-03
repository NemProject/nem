package org.nem.deploy;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.utils.*;

import java.io.InputStream;
import java.util.Properties;

/**
 * Class responsible for holding all NIS configuration settings.
 * A NIS reboot is required for configuration changes to take effect.
 */
public class NisConfiguration {
	private final Integer nodeLimit;
	private final PrivateKey bootKey;

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

		final String nodeLimit = properties.getProperty("nis.nodelimit");
		this.nodeLimit = null == nodeLimit ? null : Integer.valueOf(nodeLimit);
	}

	/**
	 * Gets the number of regular nodes that this node should communicate with during broadcasts.
	 *
	 * @return The number of regular nodes that this node should communicate with during broadcasts
	 */
	public Integer getNodeLimit() {
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
}
