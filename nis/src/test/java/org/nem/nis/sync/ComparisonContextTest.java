package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.BlockScorer;
import org.nem.nis.test.MockBlockScorer;

public class ComparisonContextTest {

	@Test
	public void comparisonContextExposesAllConstructorParameters() {
		// Arrange:
		final BlockScorer scorer = new MockBlockScorer();

		// Act:
		final ComparisonContext context = new ComparisonContext(124, 33, scorer);

		// Assert:
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(124));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(33));
		Assert.assertThat(context.getBlockScorer(), IsSame.sameInstance(scorer));
	}
}
