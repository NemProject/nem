package org.nem.deploy;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.deploy.CommonConfiguration;

import java.util.Properties;

/**
 * Class responsible for holding all NIS configuration settings.
 * A NIS reboot is required for configuration changes to take effect.
 */
public class NisConfiguration extends CommonConfiguration {
	private final int nodeLimit;
	private final PrivateKey bootKey;
	private final boolean bootWithoutAck;
	private final boolean useBinaryTransport;
	private final String bootName;

	/**
	 * Creates a new configuration object from the default properties.
	 */
	public NisConfiguration() {
		this(loadDefaultProperties());
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param properties The specified properties.
	 */
	public NisConfiguration(Properties properties) {
		super(properties);
		final String autoBootKey = getString(properties, "nis.bootKey");
		this.bootKey = null == autoBootKey? null : PrivateKey.fromHexString(autoBootKey);
		final String autoBootName = getString(properties, "nis.bootName");
		this.bootName = null == autoBootName? null : autoBootName.trim();
		this.nodeLimit = getOptionalInteger(properties, "nis.nodeLimit", 20);
		this.bootWithoutAck = getOptionalBoolean(properties, "nis.bootWithoutAck", false);
		this.useBinaryTransport = getOptionalBoolean(properties, "nis.useBinaryTransport", false);
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
	public boolean bootWithoutAck() {
		return this.bootWithoutAck;
	}

	/**
	 * Gets a value indicating whether or not this node should send binary payloads.
	 *
	 * @return true if this node should send binary payloads.
	 */
	public boolean useBinaryTransport() {
		return this.useBinaryTransport;
	}
}
