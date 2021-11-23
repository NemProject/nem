package org.nem.specific.deploy;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.NodeFeature;
import org.nem.core.test.*;
import org.nem.nis.ForkConfiguration;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class NisConfigurationTest {
	private static final List<String> REQUIRED_PROPERTY_NAMES = Arrays.asList("nem.shortServerName", "nem.httpPort", "nem.httpsPort",
			"nem.webContext", "nem.apiContext", "nem.homePath", "nem.maxThreads");

	private static final List<String> OPTIONAL_PROPERTY_NAMES = Arrays.asList("nem.folder", "nem.protocol", "nem.host", "nem.websocketPort",
			"nem.shutdownPath", "nem.useDosFilter", "nem.nonAuditedApiPaths", "nem.network", "nis.shouldAutoBoot", "nis.bootName",
			"nis.bootKey", "nis.shouldAutoHarvestOnBoot", "nis.additionalHarvesterPrivateKeys", "nis.nodeLimit", "nis.timeSyncNodeLimit",
			"nis.useBinaryTransport", "nis.useNetworkTime", "nis.ignoreFees", "nis.ipDetectionMode", "nis.unlockedLimit",
			"nis.maxTransactions", "nis.maxTransactionsPerBlock", "nis.blockGenerationTargetTime", "nis.blockChainRewriteLimit",
			"nis.transactionHashRetentionTime", "nis.additionalLocalIps", "nis.optionalFeatures", "nis.blockChainFeatures",
			"nis.allowedHarvesterAddresses", "nis.delayBlockLoading", "nis.treasuryReissuanceForkHeight",
			"nis.treasuryReissuanceForkTransactionHashes", "nis.treasuryReissuanceForkFallbackTransactionHashes");

	@Test
	public void canReadDefaultConfiguration() {
		// Arrange:
		final Properties properties = getCommonProperties();

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		assertDefaultConfiguration(config);
	}

	@Test
	public void canReadDefaultConfigurationFromResources() {
		// Act:
		final NisConfiguration config = new NisConfiguration();

		// Assert:
		assertDefaultConfiguration(config);
	}

	private static void assertDefaultConfiguration(final NisConfiguration config) {
		// Assert:
		MatcherAssert.assertThat(config.shouldAutoBoot(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.getAutoBootKey(), IsNull.nullValue());
		MatcherAssert.assertThat(config.getAutoBootName(), IsNull.nullValue());
		MatcherAssert.assertThat(config.shouldAutoHarvestOnBoot(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.getAdditionalHarvesterPrivateKeys(), IsEqual.equalTo(new PrivateKey[]{}));

		MatcherAssert.assertThat(config.getNodeLimit(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(20));
		MatcherAssert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.useNetworkTime(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.ignoreFees(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(config.getIpDetectionMode(), IsEqual.equalTo(IpDetectionMode.AutoRequired));

		MatcherAssert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(config.getTransactionHashRetentionTime(), IsEqual.equalTo(36));
		MatcherAssert.assertThat(config.getAdditionalLocalIps(), IsEqual.equalTo(new String[]{}));
		MatcherAssert.assertThat(config.getOptionalFeatures(), IsEqual.equalTo(new NodeFeature[]{
				NodeFeature.TRANSACTION_HASH_LOOKUP
		}));
		MatcherAssert.assertThat(config.getAllowedHarvesterAddresses(), IsEqual.equalTo(new Address[]{}));
		MatcherAssert.assertThat(config.delayBlockLoading(), IsEqual.equalTo(true));
		assertDefaultConfiguration(config.getBlockChainConfiguration());
	}

	private static void assertDefaultConfiguration(final BlockChainConfiguration config) {
		MatcherAssert.assertThat(config.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(10000));
		MatcherAssert.assertThat(config.getMaxTransactionsPerBlock(), IsEqual.equalTo(120));
		MatcherAssert.assertThat(config.getBlockGenerationTargetTime(), IsEqual.equalTo(60));
		MatcherAssert.assertThat(config.getBlockChainRewriteLimit(), IsEqual.equalTo(360));
		MatcherAssert.assertThat(config.getBlockChainFeatures(), IsEqual.equalTo(new BlockChainFeature[]{
				BlockChainFeature.PROOF_OF_IMPORTANCE, BlockChainFeature.WB_TIME_BASED_VESTING
		}));
	}

	@Test
	public void canReadCustomConfiguration() {
		// Arrange:
		final PrivateKey originalPrivateKey = new KeyPair().getPrivateKey();
		final PrivateKey additionalPrivateKey1 = new KeyPair().getPrivateKey();
		final PrivateKey additionalPrivateKey2 = new KeyPair().getPrivateKey();
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.shouldAutoBoot", "false");
		properties.setProperty("nis.bootKey", originalPrivateKey.toString());
		properties.setProperty("nis.bootName", "my name");
		properties.setProperty("nis.shouldAutoHarvestOnBoot", "false");
		properties.setProperty("nis.additionalHarvesterPrivateKeys",
				additionalPrivateKey1.toString() + "|" + additionalPrivateKey2.toString());
		properties.setProperty("nis.nodeLimit", "8");
		properties.setProperty("nis.timeSyncNodeLimit", "12");
		properties.setProperty("nis.useBinaryTransport", "false");
		properties.setProperty("nis.useNetworkTime", "false");
		properties.setProperty("nis.ignoreFees", "true");
		properties.setProperty("nis.ipDetectionMode", "Disabled");
		properties.setProperty("nis.unlockedLimit", "123");
		properties.setProperty("nis.maxTransactions", "980");
		properties.setProperty("nis.maxTransactionsPerBlock", "345");
		properties.setProperty("nis.blockGenerationTargetTime", "30");
		properties.setProperty("nis.blockChainRewriteLimit", "290");
		properties.setProperty("nis.transactionHashRetentionTime", "567");
		properties.setProperty("nis.additionalLocalIps", "10.0.0.10|10.0.0.20");
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		properties.setProperty("nis.blockChainFeatures", "PROOF_OF_STAKE");
		properties.setProperty("nis.allowedHarvesterAddresses", "FOO|BAR|BAZ");
		properties.setProperty("nis.delayBlockLoading", "false");
		properties.setProperty("nis.useWeightedBalances", "false");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(config.shouldAutoBoot(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(config.getAutoBootKey(), IsEqual.equalTo(originalPrivateKey));
		MatcherAssert.assertThat(config.getAutoBootName(), IsEqual.equalTo("my name"));
		MatcherAssert.assertThat(config.shouldAutoHarvestOnBoot(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(config.getAdditionalHarvesterPrivateKeys(), IsEqual.equalTo(new PrivateKey[]{
				additionalPrivateKey1, additionalPrivateKey2
		}));

		MatcherAssert.assertThat(config.getNodeLimit(), IsEqual.equalTo(8));
		MatcherAssert.assertThat(config.getTimeSyncNodeLimit(), IsEqual.equalTo(12));
		MatcherAssert.assertThat(config.useBinaryTransport(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(config.useNetworkTime(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(config.ignoreFees(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.getIpDetectionMode(), IsEqual.equalTo(IpDetectionMode.Disabled));

		MatcherAssert.assertThat(config.getUnlockedLimit(), IsEqual.equalTo(123));
		MatcherAssert.assertThat(config.getTransactionHashRetentionTime(), IsEqual.equalTo(567));
		MatcherAssert.assertThat(config.getAdditionalLocalIps(), IsEqual.equalTo(new String[]{
				"10.0.0.10", "10.0.0.20"
		}));
		MatcherAssert.assertThat(config.getOptionalFeatures(), IsEqual.equalTo(new NodeFeature[]{
				NodeFeature.TRANSACTION_HASH_LOOKUP, NodeFeature.HISTORICAL_ACCOUNT_DATA
		}));
		MatcherAssert.assertThat(config.getAllowedHarvesterAddresses(), IsEqual.equalTo(new Address[]{
				Address.fromEncoded("FOO"), Address.fromEncoded("BAR"), Address.fromEncoded("BAZ")
		}));
		MatcherAssert.assertThat(config.delayBlockLoading(), IsEqual.equalTo(false));

		assertCustomConfiguration(config.getBlockChainConfiguration());
	}

	private static void assertCustomConfiguration(final BlockChainConfiguration config) {
		MatcherAssert.assertThat(config.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(980));
		MatcherAssert.assertThat(config.getMaxTransactionsPerBlock(), IsEqual.equalTo(345));
		MatcherAssert.assertThat(config.getBlockGenerationTargetTime(), IsEqual.equalTo(30));
		MatcherAssert.assertThat(config.getBlockChainRewriteLimit(), IsEqual.equalTo(290));
		MatcherAssert.assertThat(config.getBlockChainFeatures(), IsEqual.equalTo(new BlockChainFeature[]{
				BlockChainFeature.PROOF_OF_STAKE
		}));
	}

	// endregion

	// region property required status

	@Test
	public void requiredPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new NisConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(properties.getRequiredPropertyNames(), IsEquivalent.equivalentTo(REQUIRED_PROPERTY_NAMES));
	}

	@Test
	public void optionalPropertiesAreDetectedCorrectly() {
		// Arrange:
		final MockNemProperties properties = new MockNemProperties(getCommonProperties());

		// Act:
		new NisConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(properties.getOptionalPropertyNames(), IsEquivalent.equivalentTo(OPTIONAL_PROPERTY_NAMES));
	}

	// endregion

	// region specific property tests

	@Test
	public void autoBootNameIsTrimmedOfLeadingAndTrailingWhitespace() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.bootName", " \t string with spaces\t  ");

		// Act:
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(config.getAutoBootName(), IsEqual.equalTo("string with spaces"));
	}

	// endregion

	// region node features

	@Test
	public void optionalFeaturesCannotBeParsedWithInvalidValue() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|PLACEHOLDER9");

		// Act:
		ExceptionAssert.assertThrows(v -> new NisConfiguration(properties), IllegalArgumentException.class);
	}

	@Test
	public void isFeatureSupportedReturnsTrueIfNodeFeatureArrayContainsFeature() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(config.isFeatureSupported(NodeFeature.TRANSACTION_HASH_LOOKUP), IsEqual.equalTo(true));
		MatcherAssert.assertThat(config.isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA), IsEqual.equalTo(true));
	}

	@Test
	public void isFeatureSupportedReturnsFalseIfNodeFeatureArrayDoesNotContainFeature() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.optionalFeatures", "TRANSACTION_HASH_LOOKUP|HISTORICAL_ACCOUNT_DATA");
		final NisConfiguration config = new NisConfiguration(properties);

		// Assert:
		MatcherAssert.assertThat(config.isFeatureSupported(NodeFeature.PLACEHOLDER2), IsEqual.equalTo(false));
	}

	@Test
	public void getBlockChainConfigurationReturnsExpectedDefaultConfiguration() {
		// Arrange:
		final NisConfiguration config = new NisConfiguration();

		// Act:
		final BlockChainConfiguration blockChainConfig = config.getBlockChainConfiguration();

		// Assert:
		MatcherAssert.assertThat(blockChainConfig.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(10_000));
		MatcherAssert.assertThat(blockChainConfig.getMaxTransactionsPerBlock(), IsEqual.equalTo(120));
		MatcherAssert.assertThat(blockChainConfig.getBlockGenerationTargetTime(), IsEqual.equalTo(60));
		MatcherAssert.assertThat(blockChainConfig.getBlockChainRewriteLimit(), IsEqual.equalTo(360));
	}

	@Test
	public void getBlockChainConfigurationReturnsExpectedCustomConfiguration() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.maxTransactions", "2345");
		properties.setProperty("nis.maxTransactionsPerBlock", "345");
		properties.setProperty("nis.blockGenerationTargetTime", "30");
		properties.setProperty("nis.blockChainRewriteLimit", "290");
		final NisConfiguration config = new NisConfiguration(properties);

		// Act:
		final BlockChainConfiguration blockChainConfig = config.getBlockChainConfiguration();

		// Assert:
		MatcherAssert.assertThat(blockChainConfig.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(2345));
		MatcherAssert.assertThat(blockChainConfig.getMaxTransactionsPerBlock(), IsEqual.equalTo(345));
		MatcherAssert.assertThat(blockChainConfig.getBlockGenerationTargetTime(), IsEqual.equalTo(30));
		MatcherAssert.assertThat(blockChainConfig.getBlockChainRewriteLimit(), IsEqual.equalTo(290));
	}

	@Test
	public void getForkConfigurationReturnsExpectedDefaultConfiguration() {
		// Arrange:
		final NisConfiguration config = new NisConfiguration();

		// Act:
		final ForkConfiguration forkConfig = config.getForkConfiguration();

		// Assert:
		MatcherAssert.assertThat(forkConfig.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(3481580)));
		MatcherAssert.assertThat(forkConfig.getTreasuryReissuanceForkTransactionHashes().size(), IsEqual.equalTo(52));
		MatcherAssert.assertThat(forkConfig.getTreasuryReissuanceForkFallbackTransactionHashes().size(), IsEqual.equalTo(1));
	}

	@Test
	public void getForkConfigurationReturnsExpectedCustomConfiguration() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.treasuryReissuanceForkHeight", "2345");

		final String[] hashStrings = new String[]{
				"19E0E3B991FAD3D24312A9E99D04F25C04BA3806A4F1F827B52BE403B1F6ADB9",
				"B9741D02EADFCBD714EE36B727F09F08FBA2A3744AFB3805B13E5CCE4D434AA1",
				"2F89C4126DD94C7FA29874EE12B1E9F1E51B5F89DA0445551AEE0C7BCEAD0B63"
		};
		properties.setProperty("nis.treasuryReissuanceForkTransactionHashes", String.join("|", hashStrings));

		final String[] fallbackHashStrings = new String[]{
				"2F89C4126DD94C7FA29874EE12B1E9F1E51B5F89DA0445551AEE0C7BCEAD0B63"
		};
		properties.setProperty("nis.treasuryReissuanceForkFallbackTransactionHashes", String.join("|", fallbackHashStrings));

		final NisConfiguration config = new NisConfiguration(properties);

		// Act:
		final ForkConfiguration forkConfig = config.getForkConfiguration();

		// Assert:
		MatcherAssert.assertThat(forkConfig.getTreasuryReissuanceForkHeight(), IsEqual.equalTo(new BlockHeight(2345)));
		MatcherAssert.assertThat(forkConfig.getTreasuryReissuanceForkTransactionHashes(),
				IsEqual.equalTo(Arrays.stream(hashStrings).map(Hash::fromHexString).collect(Collectors.toList())));
		MatcherAssert.assertThat(forkConfig.getTreasuryReissuanceForkFallbackTransactionHashes(),
				IsEqual.equalTo(Arrays.stream(fallbackHashStrings).map(Hash::fromHexString).collect(Collectors.toList())));
	}

	// endregion

	// region block chain features

	@Test
	public void blockChainFeaturesCannotBeParsedWithInvalidValue() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.blockChainFeatures", "PROOF_OF_UNKNOWN");

		// Act:
		ExceptionAssert.assertThrows(v -> new NisConfiguration(properties), IllegalArgumentException.class);
	}

	@Test
	public void isBlockChainFeatureSupportedReturnsTrueIfBlockChainFeatureArrayContainsFeature() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.blockChainFeatures", "PROOF_OF_STAKE");
		final BlockChainConfiguration config = new NisConfiguration(properties).getBlockChainConfiguration();

		// Assert:
		MatcherAssert.assertThat(config.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE), IsEqual.equalTo(true));
	}

	@Test
	public void isBlockChainFeatureSupportedReturnsFalseIfBlockChainFeatureArrayDoesNotContainFeature() {
		// Arrange:
		final Properties properties = getCommonProperties();
		properties.setProperty("nis.blockChainFeatures", "PROOF_OF_STAKE");
		final BlockChainConfiguration config = new NisConfiguration(properties).getBlockChainConfiguration();

		// Assert:
		MatcherAssert.assertThat(config.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_IMPORTANCE), IsEqual.equalTo(false));
	}

	// endregion

	// region sanity

	@Test
	public void configPropertiesCopiesAreConsistent() throws Exception {
		// Arrange:
		// - this is more complicated than it seems in order to make sure that the right config-default.properties is loaded
		// - use db.properties as a proxy to the production config-default.properties
		// - use test.properties as a proxy to the test config-default.properties
		final byte[] sourceConfigBytes = readAllBytes("db.properties", "config-default.properties");
		final byte[] testConfigBytes = readAllBytes("test.properties", "config-default.properties");

		// Act:
		final boolean areBuffersEqual = Arrays.equals(sourceConfigBytes, testConfigBytes);

		// Assert:
		MatcherAssert.assertThat(areBuffersEqual, IsEqual.equalTo(true));
	}

	// endregion

	private static byte[] readAllBytes(final String proxyResource, final String desiredResource) throws Exception {
		final URL proxyUrl = NisConfigurationTest.class.getClassLoader().getResource(proxyResource);
		if (null == proxyUrl) {
			throw new IllegalArgumentException(String.format("could not find: '%s'", proxyResource));
		}

		final URL url = new URL(proxyUrl.toString().replace(proxyResource, desiredResource));
		final Path resPath = java.nio.file.Paths.get(url.toURI());
		return java.nio.file.Files.readAllBytes(resPath);
	}

	private static Properties getCommonProperties() {
		final Properties properties = new Properties();
		properties.setProperty("nem.shortServerName", "Nis");
		properties.setProperty("nem.folder", "folder");
		properties.setProperty("nem.maxThreads", "1");
		properties.setProperty("nem.protocol", "ftp");
		properties.setProperty("nem.host", "10.0.0.1");
		properties.setProperty("nem.httpPort", "100");
		properties.setProperty("nem.httpsPort", "101");
		properties.setProperty("nem.websocketPort", "102");
		properties.setProperty("nem.webContext", "/web");
		properties.setProperty("nem.apiContext", "/api");
		properties.setProperty("nem.homePath", "/home");
		properties.setProperty("nem.shutdownPath", "/shutdown");
		properties.setProperty("nem.useDosFilter", "true");
		return properties;
	}
}
