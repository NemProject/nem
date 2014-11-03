package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.secret.BlockChainConstants;

public class DefaultComparisonContextTest {
	private final long EFFECTIVE_FORK_HEIGHT = BlockMarkerConstants.BETA_HARD_FORK - BlockChainConstants.BLOCKS_LIMIT;

	@Test
	public void contextHasCorrectValuesBeforeForkHeight() {
		// Act:
		final ComparisonContext context = new DefaultComparisonContext(new BlockHeight(EFFECTIVE_FORK_HEIGHT - 1));

		// Assert:
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(BlockChainConstants.REWRITE_LIMIT));
	}

	@Test
	public void contextHasCorrectValuesAtForkHeight() {
		// Act:
		final ComparisonContext context = new DefaultComparisonContext(new BlockHeight(EFFECTIVE_FORK_HEIGHT));

		// Assert:
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(BlockChainConstants.REWRITE_LIMIT));
	}

	@Test
	public void contextHasCorrectValuesAfterForkHeight() {
		// Act:
		final ComparisonContext context = new DefaultComparisonContext(new BlockHeight(EFFECTIVE_FORK_HEIGHT + 1));

		// Assert:
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(BlockChainConstants.REWRITE_LIMIT));
	}
}