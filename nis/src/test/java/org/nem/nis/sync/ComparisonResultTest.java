package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;

public class ComparisonResultTest {

	@Test
	public void comparisonResultExposesAllConstructorParameters() {
		// Act:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED, 33, true);

		// Assert:
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		Assert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(33L));
		Assert.assertThat(result.areChainsConsistent(), IsSame.sameInstance(true));
	}


	@Test(expected = UnsupportedOperationException.class)
	public void commonBlockHeightCannotBeAccessedWhenCodeIsNotRemoteNotSynced() {
		// Arrange:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_IS_SYNCED, 33, true);

		// Act:
		result.getCommonBlockHeight();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void areChainsConsistentCannotBeAccessedWhenCodeIsNotRemoteNotSynced() {
		// Arrange:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_IS_SYNCED, 33, true);

		// Act:
		result.areChainsConsistent();
	}
}
