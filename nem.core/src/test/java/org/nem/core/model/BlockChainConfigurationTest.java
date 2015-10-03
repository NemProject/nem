package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.Utils;

public class BlockChainConfigurationTest {

	// region transactions per sync attempt

	@Test
	public void getMinTransactionsPerSyncAttemptReturnsMaxTransactionsPerBlock() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getMinTransactionsPerSyncAttempt(), IsEqual.equalTo(100));
	}

	@Test
	public void getDefaultTransactionsPerSyncAttemptReturnsHalfOfMaximum() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getDefaultTransactionsPerSyncAttempt(), IsEqual.equalTo(500));
	}

	// endregion

	// region blocks per sync attempt

	@Test
	public void getDefaultBlocksPerSyncAttemptReturnsOneFourthOfSyncBlockLimit() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getDefaultBlocksPerSyncAttempt(), IsEqual.equalTo(70 / 4));
	}

	@Test
	public void getMaxBlocksPerSyncAttemptReturnsRewriteLimitPlusForty() {
		// Arrange:
		final BlockChainConfiguration configuration = createConfiguration();

		// Assert:
		Assert.assertThat(configuration.getMaxBlocksPerSyncAttempt(), IsEqual.equalTo(30 + 40));
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
		return Utils.createBlockChainConfiguration(1000, 100, 45, 30);
	}

	private static BlockChainConfiguration createConfigurationWithFeature(final BlockChainFeature feature) {
		return Utils.createBlockChainConfiguration(1000, 100, 45, 30, new BlockChainFeature[] { feature });
	}
}
