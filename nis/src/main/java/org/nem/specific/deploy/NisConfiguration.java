package org.nem.specific.deploy;

import org.nem.core.crypto.PrivateKey;
import org.nem.core.model.*;
import org.nem.core.node.NodeFeature;
import org.nem.deploy.CommonConfiguration;
import org.nem.nis.ForkConfiguration;

import java.util.*;

/**
 * Class responsible for holding all NIS configuration settings. A NIS reboot is required for configuration changes to take effect.
 */
public class NisConfiguration extends CommonConfiguration {
	private final boolean shouldAutoBoot;
	private final PrivateKey bootKey;
	private final String bootName;
	private final boolean shouldAutoHarvestOnBoot;
	private final PrivateKey[] additionalHarvesterPrivateKeys;
	private final int nodeLimit;
	private final int timeSyncNodeLimit;
	private final boolean useBinaryTransport;
	private final boolean useNetworkTime;
	private final boolean ignoreFees;
	private final IpDetectionMode ipDetectionMode;
	private final int unlockedLimit;
	private final int transactionHashRetentionTime;
	private final String[] additionalLocalIps;
	private final NodeFeature[] optionalFeatures;
	private final Address[] allowedHarvesterAddresses;
	private final boolean delayBlockLoading;
	private final BlockChainConfiguration blockChainConfiguration;
	private final ForkConfiguration forkConfiguration;

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
		this.shouldAutoBoot = properties.getOptionalBoolean("nis.shouldAutoBoot", true);
		final String autoBootKey = properties.getOptionalString("nis.bootKey", null);
		this.bootKey = null == autoBootKey ? null : PrivateKey.fromHexString(autoBootKey);

		final String autoBootName = properties.getOptionalString("nis.bootName", null);
		this.bootName = null == autoBootName ? null : autoBootName.trim();

		this.shouldAutoHarvestOnBoot = properties.getOptionalBoolean("nis.shouldAutoHarvestOnBoot", true);
		this.additionalHarvesterPrivateKeys = Arrays.stream(properties.getOptionalStringArray("nis.additionalHarvesterPrivateKeys", ""))
				.map(PrivateKey::fromHexString).toArray(PrivateKey[]::new);

		this.nodeLimit = properties.getOptionalInteger("nis.nodeLimit", 5);
		this.timeSyncNodeLimit = properties.getOptionalInteger("nis.timeSyncNodeLimit", 20);
		this.useBinaryTransport = properties.getOptionalBoolean("nis.useBinaryTransport", true);
		this.useNetworkTime = properties.getOptionalBoolean("nis.useNetworkTime", true);
		this.ignoreFees = properties.getOptionalBoolean("nis.ignoreFees", false);

		final String ipDetectionMode = properties.getOptionalString("nis.ipDetectionMode", null);
		this.ipDetectionMode = null == ipDetectionMode ? IpDetectionMode.AutoRequired : IpDetectionMode.valueOf(ipDetectionMode);

		this.unlockedLimit = properties.getOptionalInteger("nis.unlockedLimit", 4);

		this.transactionHashRetentionTime = properties.getOptionalInteger("nis.transactionHashRetentionTime", 36);
		this.additionalLocalIps = properties.getOptionalStringArray("nis.additionalLocalIps", "");

		this.optionalFeatures = Arrays.stream(properties.getOptionalStringArray("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP"))
				.map(NodeFeature::fromString).toArray(NodeFeature[]::new);

		this.allowedHarvesterAddresses = Arrays.stream(properties.getOptionalStringArray("nis.allowedHarvesterAddresses", ""))
				.map(Address::fromEncoded).toArray(Address[]::new);

		this.delayBlockLoading = properties.getOptionalBoolean("nis.delayBlockLoading", true);

		this.blockChainConfiguration = parseBlockChainConfiguration(properties);
		this.forkConfiguration = new ForkConfiguration(properties);
	}

	private static BlockChainConfiguration parseBlockChainConfiguration(final NemProperties properties) {
		final String defaultBlockChainFeatures = "PROOF_OF_IMPORTANCE|WB_TIME_BASED_VESTING";
		final BlockChainFeature[] blockChainFeatures = Arrays
				.stream(properties.getOptionalStringArray("nis.blockChainFeatures", defaultBlockChainFeatures))
				.map(BlockChainFeature::fromString).toArray(BlockChainFeature[]::new);
		return new BlockChainConfigurationBuilder()
				.setMaxTransactionsPerSyncAttempt(properties.getOptionalInteger("nis.maxTransactions", 10000))
				.setMaxTransactionsPerBlock(properties.getOptionalInteger("nis.maxTransactionsPerBlock", 120))
				.setBlockGenerationTargetTime(properties.getOptionalInteger("nis.blockGenerationTargetTime", 60))
				.setBlockChainRewriteLimit(properties.getOptionalInteger("nis.blockChainRewriteLimit", 360))
				.setBlockChainFeatures(blockChainFeatures).build();
	}

	// region boot / harvest

	/**
	 * Gets a value indicating whether or not this node should automatically boot even if no key and boot name is specified.
	 *
	 * @return true if booting should automatically take place.
	 */
	public boolean shouldAutoBoot() {
		return this.shouldAutoBoot;
	}

	/**
	 * Gets the private key of the auto-boot node. If null, the node shouldn't auto-boot.
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

	// endregion

	// region network communication

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
	 * Gets a value indicating whether or not this node should use the network time for time stamps in NIS entities (e.g. blocks,
	 * transactions, ...).
	 *
	 * @return true if this node should use the network time.
	 */
	public boolean useNetworkTime() {
		return this.useNetworkTime;
	}

	/**
	 * Gets a value indicating whether or not the transactions fees should be ignored.
	 *
	 * @return true if transactions fees should be ignored.
	 */
	public boolean ignoreFees() {
		return this.ignoreFees;
	}

	/**
	 * Gets the IP detection mode.
	 *
	 * @return The IP detection mode.
	 */
	public IpDetectionMode getIpDetectionMode() {
		return this.ipDetectionMode;
	}

	// endregion

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
	 * Gets the block chain configuration.
	 *
	 * @return The block chain configuration.
	 */
	public BlockChainConfiguration getBlockChainConfiguration() {
		return this.blockChainConfiguration;
	}

	/**
	 * Gets the fork configuration.
	 *
	 * @return The fork configuration.
	 */
	public ForkConfiguration getForkConfiguration() {
		return this.forkConfiguration;
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
	 * Gets a value indicating whether or not the node supports the specified feature.
	 *
	 * @return true if the node supports the specified feature.
	 */
	public boolean isFeatureSupported(final NodeFeature feature) {
		return Arrays.stream(this.getOptionalFeatures()).anyMatch(f -> f == feature);
	}
}
