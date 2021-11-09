package org.nem.nis.sync;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

public class ComparisonContextTest {

	@Test
	public void comparisonContextCanBeCreatedWithMaxBlocksToAnalyzeGreaterThanMaxBlocksToRewrite() {
		// Act:
		final ComparisonContext context = new ComparisonContext(124, 33);

		// Assert:
		MatcherAssert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(124));
		MatcherAssert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(33));
	}

	@Test
	public void comparisonContextCanBeCreatedWithMaxBlocksToAnalyzeLessThanMaxBlocksToRewrite() {
		// Act:
		final ComparisonContext context = new ComparisonContext(33, 124);

		// Assert:
		MatcherAssert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(33));
		MatcherAssert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(124));
	}
}
