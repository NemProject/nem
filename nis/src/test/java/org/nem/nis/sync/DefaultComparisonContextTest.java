package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.BlockChainConstants;
import org.nem.core.model.primitive.BlockHeight;

public class DefaultComparisonContextTest {

	@Test
	public void contextHasCorrectValues() {
		assertContextHasCorrectValues(1);
		assertContextHasCorrectValues(1234);
		assertContextHasCorrectValues(1_000_000);
	}

	private static void assertContextHasCorrectValues(final long height) {
		// Act:
		final ComparisonContext context = new DefaultComparisonContext(new BlockHeight(height));

		// Assert:
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(BlockChainConstants.BLOCKS_LIMIT));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(BlockChainConstants.DEFAULT_REWRITE_LIMIT));
	}
}