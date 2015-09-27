package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.*;

public class BlockChainConfigurationBuilderTest {

	@Test
	public void canCreateDefaultBlockChainConfiguration() {
		// Act:
		final BlockChainConfiguration configuration = new BlockChainConfigurationBuilder().build();

		// Assert:
		Assert.assertThat(configuration.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(10000));
		Assert.assertThat(configuration.getMaxTransactionsPerBlock(), IsEqual.equalTo(120));
		Assert.assertThat(configuration.getBlockGenerationTargetTime(), IsEqual.equalTo(60));
		Assert.assertThat(configuration.getBlockChainRewriteLimit(), IsEqual.equalTo(360));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_IMPORTANCE), IsEqual.equalTo(true));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE), IsEqual.equalTo(false));
		Assert.assertThat(configuration.isBlockChainFeatureSupported(BlockChainFeature.STABILIZE_BLOCK_TIMES), IsEqual.equalTo(false));
	}

	@Test
	public void canCreateCustomBlockChainConfigurationWithDefaultFeatures() {
		// Act:
		final BlockChainConfigurationBuilder builder = new BlockChainConfigurationBuilder();
		builder.setMaxTransactionsPerSyncAttempt(1000);
		builder.setMaxTransactionsPerBlock(100);
		builder.setBlockGenerationTargetTime(45);
		builder.setBlockChainRewriteLimit(30);
		final BlockChainConfiguration configuration = builder.build();

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
	public void canCreateCustomBlockChainConfigurationWithCustomFeatures() {
		// Act:
		final BlockChainConfigurationBuilder builder = new BlockChainConfigurationBuilder();
		builder.setMaxTransactionsPerSyncAttempt(1000);
		builder.setMaxTransactionsPerBlock(100);
		builder.setBlockGenerationTargetTime(45);
		builder.setBlockChainRewriteLimit(30);
		builder.setBlockChainFeatures(new BlockChainFeature[] { BlockChainFeature.PROOF_OF_STAKE });
		final BlockChainConfiguration configuration = builder.build();

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
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(100, 1000, 45, 30), IllegalArgumentException.class);

		// maxTransactionsPerSyncAttempt > maxTransactionsPerBlock * blockChainRewriteLimit
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(301, 10, 45, 30), IllegalArgumentException.class);

		// maxTransactionsPerBlock < 1
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 0, 45, 30), IllegalArgumentException.class);

		// maxTransactionsPerBlock > 10_000
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 10_001, 45, 30), IllegalArgumentException.class);

		// blockGenerationTargetTime < 10 seconds
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 100, 9, 30), IllegalArgumentException.class);

		// blockGenerationTargetTime > 1 day
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 100, 86401, 30), IllegalArgumentException.class);

		// blockChainRewriteLimit < 10
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 100, 45, 9), IllegalArgumentException.class);

		// blockChainRewriteLimit > estimated blocks per day
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 100, 45, 1921), IllegalArgumentException.class);

		// blockChainRewriteLimit > estimated blocks per day
		ExceptionAssert.assertThrows(v -> Utils.createBlockChainConfiguration(1000, 100, 45, 30, null), IllegalArgumentException.class);
	}

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

	private static BlockChainConfiguration createConfigurationWithFeature(final BlockChainFeature feature) {
		return Utils.createBlockChainConfiguration(1000, 100, 45, 30, new BlockChainFeature[] { feature });
	}

	// endregion
}