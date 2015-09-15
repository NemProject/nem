package org.nem.specific.deploy;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.model.*;
import org.nem.core.node.NodeFeature;
import org.nem.deploy.CommonConfiguration;

import java.util.*;

/**
 * Class responsible for holding all NIS configuration settings.
 * A NIS reboot is required for configuration changes to take effect.
 */
public class NisConfiguration extends CommonConfiguration {
	private final PrivateKey bootKey;
	private final String bootName;
	private final boolean shouldAutoHarvestOnBoot;
	private final PrivateKey[] additionalHarvesterPrivateKeys;
	private final int nodeLimit;
	private final int timeSyncNodeLimit;
	private final boolean useBinaryTransport;
	private final boolean useNetworkTime;
	private final IpDetectionMode ipDetectionMode;
	private final int unlockedLimit;
	private final int maxTransactions;
	private final int maxTransactionsPerBlock;
	private final int transactionHashRetentionTime;
	private final String[] additionalLocalIps;
	private final NodeFeature[] optionalFeatures;
	private final Address[] allowedHarvesterAddresses;
	private final boolean delayBlockLoading;
	private final boolean useWeightedBalances;

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
		this(new NemProperties(properties));
	}

	/**
	 * Creates a new configuration object around the specified properties.
	 *
	 * @param properties The specified properties.
	 */
	public NisConfiguration(final NemProperties properties) {
		super(properties);
		final String autoBootKey = properties.getOptionalString("nis.bootKey", null);
		this.bootKey = null == autoBootKey ? null : PrivateKey.fromHexString(autoBootKey);

		final String autoBootName = properties.getOptionalString("nis.bootName", null);
		this.bootName = null == autoBootName ? null : autoBootName.trim();

		this.shouldAutoHarvestOnBoot = properties.getOptionalBoolean("nis.shouldAutoHarvestOnBoot", true);
		this.additionalHarvesterPrivateKeys = Arrays.stream(properties.getOptionalStringArray("nis.additionalHarvesterPrivateKeys", ""))
				.map(PrivateKey::fromHexString)
				.toArray(PrivateKey[]::new);

		this.nodeLimit = properties.getOptionalInteger("nis.nodeLimit", 5);
		this.timeSyncNodeLimit = properties.getOptionalInteger("nis.timeSyncNodeLimit", 20);
		this.useBinaryTransport = properties.getOptionalBoolean("nis.useBinaryTransport", true);
		this.useNetworkTime = properties.getOptionalBoolean("nis.useNetworkTime", true);

		final String ipDetectionMode = properties.getOptionalString("nis.ipDetectionMode", null);
		this.ipDetectionMode = null == ipDetectionMode
				? IpDetectionMode.AutoRequired
				: IpDetectionMode.valueOf(ipDetectionMode);

		this.unlockedLimit = properties.getOptionalInteger("nis.unlockedLimit", 1);
		this.maxTransactions = properties.getOptionalInteger("nis.maxTransactions", 10000);
		this.maxTransactionsPerBlock = properties.getOptionalInteger("nis.maxTransactionsPerBlock", 120);
		this.transactionHashRetentionTime = properties.getOptionalInteger("nis.transactionHashRetentionTime", 36);
		this.additionalLocalIps = properties.getOptionalStringArray("nis.additionalLocalIps", "");

		this.optionalFeatures = Arrays.stream(properties.getOptionalStringArray("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP"))
				.map(NodeFeature::fromString)
				.toArray(NodeFeature[]::new);

		this.allowedHarvesterAddresses = Arrays.stream(properties.getOptionalStringArray("nis.allowedHarvesterAddresses", ""))
				.map(Address::fromEncoded)
				.toArray(Address[]::new);

		this.delayBlockLoading = properties.getOptionalBoolean("nis.delayBlockLoading", true);

		// TODO 20150913 J-B: if you're planning on adding a bunch of features like this (e.g. usePoi, useEigenTrust, ...),
		// > i'd rather use an enum (similar to NodeFeatures) and have a single property like blockChainFeatures
		// > also, given the renames 'useTimeBasedVesting' might be a better name, but wanted to check with you
		this.useWeightedBalances = properties.getOptionalBoolean("nis.useWeightedBalances", true);
	}

	//region boot / harvest

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
	 * Gets a value indicating whether or not this node should automatically start harvesting when booted.
	 *
	 * @return true if booting should automatically start harvesting.
	 */
	public boolean shouldAutoHarvestOnBoot() {
		return this.shouldAutoHarvestOnBoot;
	}

	/**
	 * Gets additional private keys that should automatically start harvesting on boot.
	 *
	 * @return Additional private keys that should automatically start harvesting on boot.
	 */
	public PrivateKey[] getAdditionalHarvesterPrivateKeys() {
		return this.additionalHarvesterPrivateKeys;
	}

	//endregion

	//region network communication

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

	//endregion

	/**
	 * Gets the maximum number of accounts that can simultaneously harvest.
	 *
	 * @return The maximum number of accounts that can simultaneously harvest.
	 */
	public int getUnlockedLimit() {
		return this.unlockedLimit;
	}

	/**
	 * Gets the number of hours that the transaction hashes are kept in the cache.
	 *
	 * @return The number of hours.
	 */
	public int getTransactionHashRetentionTime() {
		return this.transactionHashRetentionTime;
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
	 * Gets the maximum number of transactions that are allowed in a single block.
	 *
	 * @return The maximum number of transactions per block.
	 */
	public int getMaxTransactionsPerBlock() {
		return this.maxTransactionsPerBlock;
	}

	/**
	 * Gets the additional IPs that should be treated as local.
	 *
	 * @return The additional IPs that should be treated as local.
	 */
	public String[] getAdditionalLocalIps() {
		return this.additionalLocalIps;
	}

	/**
	 * Gets the optional node features.
	 *
	 * @return The optional features.
	 */
	public NodeFeature[] getOptionalFeatures() {
		return this.optionalFeatures;
	}

	/**
	 * Gets the allowed harvester addresses.
	 *
	 * @return The allowed harvester addresses.
	 */
	public Address[] getAllowedHarvesterAddresses() {
		return this.allowedHarvesterAddresses;
	}

	/**
	 * Gets a value indicating whether or not block loading should be delayed during startup.
	 *
	 * @return true if the block loading should be delayed.
	 */
	public boolean delayBlockLoading() {
		return this.delayBlockLoading;
	}

	/**
	 * Gets a value indicating whether or not NIS should use weighted balances.
	 * If false, NIS immediately vests all balances (only recommended for private chains).
	 *
	 * @return true if NIS should use weighted balances.
	 */
	public boolean useWeightedBalances() {
		return this.useWeightedBalances;
	}

	/**
	 * Gets a value indicating whether or not the node supports the specified feature.
	 *
	 * @return true if the node supports the specified feature.
	 */
	public boolean isFeatureSupported(final NodeFeature feature) {
		return Arrays.stream(this.getOptionalFeatures()).anyMatch(f -> f == feature);
	}
}
