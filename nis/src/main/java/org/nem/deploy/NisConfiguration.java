package org.nem.deploy;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.deploy.CommonConfiguration;
import org.nem.core.utils.StringUtils;

import java.util.Properties;

/**
 * Class responsible for holding all NIS configuration settings.
 * A NIS reboot is required for configuration changes to take effect.
 */
public class NisConfiguration extends CommonConfiguration {
	private final int nodeLimit;
	private final int timeSyncNodeLimit;
	private final PrivateKey bootKey;
	private final boolean useBinaryTransport;
	private final String bootName;
	private final boolean useNetworkTime;
	private final IpDetectionMode ipDetectionMode;
	private final int unlockedLimit;
	private final String[] nonAuditedApiPaths;
	private final int maxTransactions;
	private final String[] additionalLocalIps;

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
	public NisConfiguration(final Properties properties) {
		super(properties);
		final String autoBootKey = getOptionalString(properties, "nis.bootKey", null);
		this.bootKey = null == autoBootKey ? null : PrivateKey.fromHexString(autoBootKey);

		final String autoBootName = getOptionalString(properties, "nis.bootName", null);
		this.bootName = null == autoBootName ? null : autoBootName.trim();

		this.nodeLimit = getOptionalInteger(properties, "nis.nodeLimit", 5);
		this.timeSyncNodeLimit = getOptionalInteger(properties, "nis.timeSyncNodeLimit", 20);
		this.useBinaryTransport = getOptionalBoolean(properties, "nis.useBinaryTransport", true);
		this.useNetworkTime = getOptionalBoolean(properties, "nis.useNetworkTime", true);

		final String ipDetectionMode = getOptionalString(properties, "nis.ipDetectionMode", null);
		this.ipDetectionMode = null == ipDetectionMode
				? IpDetectionMode.AutoRequired
				: IpDetectionMode.valueOf(ipDetectionMode);

		this.unlockedLimit = getOptionalInteger(properties, "nis.unlockedLimit", 1);
		this.nonAuditedApiPaths = getOptionalStringArray(properties, "nis.nonAuditedApiPaths", "/heartbeat|/status|/chain/height");
		this.maxTransactions = getOptionalInteger(properties, "nis.maxTransactions", 10000);
		this.additionalLocalIps = getOptionalStringArray(properties, "nis.additionalLocalIps", "");
	}

	private static String[] getOptionalStringArray(final Properties properties, final String key, final String defaultValue) {
		final String stringArray = getOptionalString(properties, key, defaultValue);
		return StringUtils.isNullOrWhitespace(stringArray) ? new String[] { } : stringArray.split("\\|");
	}

	/**
	 * Gets the number of regular nodes that this node should communicate with during broadcasts.
	 *
	 * @return The number of regular nodes that this node should communicate with during broadcasts.
	 */
	public int getNodeLimit() {
		return this.nodeLimit;
	}

	/**
	 * Gets the number of regular nodes that this node should communicate with during time synchronization.
	 *
	 * @return The number of regular nodes that this node should communicate with during time synchronization.
	 */
	public int getTimeSyncNodeLimit() {
		return this.timeSyncNodeLimit;
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
	 * Gets a value indicating whether or not this node should send binary payloads.
	 *
	 * @return true if this node should send binary payloads.
	 */
	public boolean useBinaryTransport() {
		return this.useBinaryTransport;
	}

	/**
	 * Gets a value indicating whether or not this node should use the network time for time stamps in NIS entities
	 * (e.g. blocks, transactions, ...).
	 *
	 * @return true if this node should use the network time.
	 */
	public boolean useNetworkTime() {
		return this.useNetworkTime;
	}

	/**
	 * Gets the IP detection mode.
	 *
	 * @return The IP detection mode.
	 */
	public IpDetectionMode getIpDetectionMode() {
		return this.ipDetectionMode;
	}

	/**
	 * Gets the number of regular nodes that this node should communicate with during broadcasts.
	 *
	 * @return The number of regular nodes that this node should communicate with during broadcasts.
	 */
	public int getUnlockedLimit() {
		return this.unlockedLimit;
	}

	/**
	 * Gets the NIS APIs that shouldn't be audited.
	 *
	 * @return The NIS APIs that shouldn't be audited.
	 */
	public String[] getNonAuditedApiPaths() {
		return this.nonAuditedApiPaths;
	}

	/**
	 * Gets the maximum number of transactions that should be inside the blocks returned in the /chain/blocks-after request.
	 *
	 * @return The maximum number of transactions.
	 */
	public int getMaxTransactions() {
		return this.maxTransactions;
	}

	/**
	 * Gets the additional IPs that should be treated as local.
	 *
	 * @return The additional IPs that should be treated as local.
	 */
	public String[] getAdditionalLocalIps() {
		return this.additionalLocalIps;
	}
}
