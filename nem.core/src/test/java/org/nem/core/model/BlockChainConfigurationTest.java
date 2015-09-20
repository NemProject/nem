package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.test.ExceptionAssert;

public class BlockChainConfigurationTest {

	// region ctor

	@Test
	public void canCreateBlockChainConfigurationWithDefaultFeatures() {
		// Act:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(1000));
		Assert.assertThat(configuration.getMaxTransactionsPerBlock(), IsEqual.equalTo(100));
		Assert.assertThat(configuration.getBlockGenerationTargetTime(), IsEqual.equalTo(45));
		Assert.assertThat(configuration.getBlockChainRewriteLimit(), IsEqual.equalTo(30));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_IMPORTANCE), IsEqual.equalTo(true));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE), IsEqual.equalTo(false));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.STABILIZE_BLOCK_TIMES), IsEqual.equalTo(false));
	}

	@Test
	public void canCreateBlockChainConfigurationWithCustomFeatures() {
		// Act:
		final BlockChainConfiguration configuration = createConfigurationWithFeature(BlockChainFeature.PROOF_OF_STAKE);

		// Assert:
		Assert.assertThat(configuration.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(1000));
		Assert.assertThat(configuration.getMaxTransactionsPerBlock(), IsEqual.equalTo(100));
		Assert.assertThat(configuration.getBlockGenerationTargetTime(), IsEqual.equalTo(45));
		Assert.assertThat(configuration.getBlockChainRewriteLimit(), IsEqual.equalTo(30));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_IMPORTANCE), IsEqual.equalTo(false));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE), IsEqual.equalTo(true));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.STABILIZE_BLOCK_TIMES), IsEqual.equalTo(false));
	}

	@Test
	public void cannotCreateBlockChainConfigurationFromInvalidData() {
		// Assert:
		// maxTransactionsPerSyncAttempt < maxTransactionsPerBlock
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(100, 1000, 45, 30), IllegalArgumentException.class);

		// maxTransactionsPerSyncAttempt > maxTransactionsPerBlock * blockChainRewriteLimit
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(301, 10, 45, 30), IllegalArgumentException.class);

		// maxTransactionsPerBlock < 1
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 0, 45, 30), IllegalArgumentException.class);

		// maxTransactionsPerBlock > 10_000
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 10_001, 45, 30), IllegalArgumentException.class);

		// blockGenerationTargetTime < 10 seconds
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 100, 9, 30), IllegalArgumentException.class);

		// blockGenerationTargetTime > 1 day
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 100, 86401, 30), IllegalArgumentException.class);

		// blockChainRewriteLimit < 10
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 100, 45, 9), IllegalArgumentException.class);

		// blockChainRewriteLimit > estimated blocks per day
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 100, 45, 1921), IllegalArgumentException.class);

		// blockChainRewriteLimit > estimated blocks per day
		ExceptionAssert.assertThrows(v -> new BlockChainConfiguration(1000, 100, 45, 30, null), IllegalArgumentException.class);
	}

	// endregion

	// region transactions per sync attempt

	@Test
	public void getDefaultMaxTransactionsPerSyncAttemptReturnsHalfOfMaximum() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getDefaultMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(500));
	}

	@Test
	public void getMinTransactionsPerSyncAttemptReturnsMaxTransactionsPerBlock() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getMinTransactionsPerSyncAttempt(), IsEqual.equalTo(100));
	}

	// endregion

	// region getSyncBlockLimit

	@Test
	public void getSyncBlockLimitReturnsRewriteLimitPlusForty() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getSyncBlockLimit(), IsEqual.equalTo(30 + 40));
	}

	// endregion

	// region getEstimatedBlocksPerX

	@Test
	public void getEstimatedBlocksPerDayReturnsNumberOfBlocksPerDay() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getEstimatedBlocksPerDay(), IsEqual.equalTo(1920));
	}

	@Test
	public void getEstimatedBlocksPerMonthReturnsNumberOfBlocksPerMonth() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getEstimatedBlocksPerMonth(), IsEqual.equalTo(1920 * 30));
	}

	@Test
	public void getEstimatedBlocksPerYearReturnsNumberOfBlocksPerYear() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getEstimatedBlocksPerYear(), IsEqual.equalTo(1920 * 365));
	}

	// endregion

	// region isBlockChainFeatureSupported

	@Test
	public void isBlockChainFeatureSupportedReturnsTrueIfFeatureIsSupported() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfigurationWithFeature(BlockChainFeature.PROOF_OF_STAKE);

		// Assert:
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE), IsEqual.equalTo(true));
	}

	@Test
	public void isBlockChainFeatureSupportedReturnsFalseIfFeatureIsNotSupported() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfigurationWithFeature(BlockChainFeature.PROOF_OF_STAKE);

		// Assert:
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_IMPORTANCE), IsEqual.equalTo(false));
	}

	// endregion

	private static BlockChainConfiguration createConfiguration() {
		return new BlockChainConfiguration(1000, 100, 45, 30);
	}

	private static BlockChainConfiguration createConfigurationWithFeature(final BlockChainFeature feature) {
		return new BlockChainConfiguration(1000, 100, 45, 30, BlockChainFeature.explode(feature.value()));
	}
}
