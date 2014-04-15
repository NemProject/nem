package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.utils.ArrayUtils;

	public class ComparisonResultTest {

	@Test
	public void comparisonResultExposesAllConstructorParameters() {
		// Act:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED, 33, true, 66L);

		// Assert:
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		Assert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(33L));
		Assert.assertThat(result.areChainsConsistent(), IsSame.sameInstance(true));
		Assert.assertThat(result.getRemoteHeight(), IsEqual.equalTo(66L));
	}


	@Test(expected = UnsupportedOperationException.class)
	public void commonBlockHeightCannotBeAccessedWhenCodeIsNotRemoteNotSynced() {
		// Arrange:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_IS_SYNCED, 33, true, 66L);

		// Act:
		result.getCommonBlockHeight();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void areChainsConsistentCannotBeAccessedWhenCodeIsNotRemoteNotSynced() {
		// Arrange:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_IS_SYNCED, 33, true, 66L);

		// Act:
		result.areChainsConsistent();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getRemoteHeightCannotBeAccessedWhenCodeIsRemoteHasNoBlocks() {
		// Arrange:
		final ComparisonResult result = new ComparisonResult(ComparisonResult.Code.REMOTE_HAS_NO_BLOCKS, 33, true, 66L);

		// Act:
		result.getRemoteHeight();
	}
}
