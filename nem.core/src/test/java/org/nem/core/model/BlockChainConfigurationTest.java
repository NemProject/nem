package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.test.ExceptionAssert;

public class BlockChainConfigurationTest {

	// region ctor

	@Test
	public void canCreateBlockChainConfiguration() {
		// Act:
		final BlockChainConfiguration configuration = new BlockChainConfiguration(1000, 100, 45, 30);

		// Assert:
		Assert.assertThat(configuration.getMaxTransactionsPerSyncAttempt(), IsEqual.equalTo(1000));
		Assert.assertThat(configuration.getMaxTransactionsPerBlock(), IsEqual.equalTo(100));
		Assert.assertThat(configuration.getBlockGenerationTargetTime(), IsEqual.equalTo(45));
		Assert.assertThat(configuration.getBlockChainRewriteLimit(), IsEqual.equalTo(30));
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
	}

	// endregion

	// region getEstimatedBlocksPerDay

	@Test
	public void getEstimatedBlocksPerDayReturnsNumberOfBlocksPerDay() {
		// Arrange:
		final BlockChainConfiguration configuration = new BlockChainConfiguration(1000, 100, 45, 30);

		// Assert:
		Assert.assertThat(configuration.getEstimatedBlocksPerDay(), IsEqual.equalTo(1920));
	}

	// endregion
}
