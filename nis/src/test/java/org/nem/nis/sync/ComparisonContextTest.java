package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;

import java.security.InvalidParameterException;

public class ComparisonContextTest {

	@Test
	public void comparisonContextExposesAllConstructorParameters() {
		// Act:
		final ComparisonContext context = new ComparisonContext(124, 33);

		// Assert:
		Assert.assertThat(context.getMaxNumBlocksToAnalyze(), IsEqual.equalTo(124));
		Assert.assertThat(context.getMaxNumBlocksToRewrite(), IsEqual.equalTo(33));
	}

	@Test(expected = InvalidParameterException.class)
	public void maxBlocksToAnalyzeCannotBeLessThanMaxBlocksToRewrite() {
		// Act:
		new ComparisonContext(123, 124);
	}

	@Test(expected = InvalidParameterException.class)
	public void maxBlocksToAnalyzeCannotBeEqualToMaxBlocksToRewrite() {
		// Act:
		new ComparisonContext(124, 124);
	}
}
