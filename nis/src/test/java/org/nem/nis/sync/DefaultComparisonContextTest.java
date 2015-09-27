package org.nem.nis.sync;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.test.NisTestConstants;

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
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(NisTestConstants.BLOCKS_LIMIT));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(NisTestConstants.REWRITE_LIMIT));
	}
}