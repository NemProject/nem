package org.nem.nis.sync;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.BlockHeight;

public class ComparisonResultTest {

	//region Code

	@Test
	public void codeGetValueReturnsUnderlyingValue() {
		// Act:
		final ComparisonResult.Code code = ComparisonResult.Code.REMOTE_REPORTED_EQUAL_CHAIN_SCORE;

		// Assert:
		Assert.assertThat(code.getValue(), IsEqual.equalTo(5));
	}

	@Test
	public void isEvilOnlyReturnsTrueForEvilCodes() {
		for (final ComparisonResult.Code code : ComparisonResult.Code.values()) {
			// Assert: (check negative as an alternative to high bit check)
			Assert.assertThat(code.isEvil(), IsEqual.equalTo(code.getValue() < 0));
		}
	}

	//endregion

	@Test
	public void comparisonResultExposesAllConstructorParameters() {
		// Act:
		final ComparisonResult result = new ComparisonResult(
				ComparisonResult.Code.REMOTE_IS_NOT_SYNCED,
				33,
				true,
				new BlockHeight(66));

		// Assert:
		Assert.assertThat(result.getCode(), IsEqual.equalTo(ComparisonResult.Code.REMOTE_IS_NOT_SYNCED));
		Assert.assertThat(result.getCommonBlockHeight(), IsEqual.equalTo(33L));
		Assert.assertThat(result.areChainsConsistent(), IsEqual.equalTo(true));
		Assert.assertThat(result.getRemoteHeight(), IsEqual.equalTo(new BlockHeight(66)));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void commonBlockHeightCannotBeAccessedWhenCodeIsNotRemoteNotSynced() {
		// Arrange:
		final ComparisonResult result = createResultWithCode(ComparisonResult.Code.REMOTE_IS_SYNCED);

		// Act:
		result.getCommonBlockHeight();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void areChainsConsistentCannotBeAccessedWhenCodeIsNotRemoteNotSynced() {
		// Arrange:
		final ComparisonResult result = createResultWithCode(ComparisonResult.Code.REMOTE_IS_SYNCED);

		// Act:
		result.areChainsConsistent();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getRemoteHeightCannotBeAccessedWhenCodeIsRemoteHasNoBlocks() {
		// Arrange:
		final ComparisonResult result = createResultWithCode(ComparisonResult.Code.REMOTE_HAS_NO_BLOCKS);

		// Act:
		result.getRemoteHeight();
	}

	private static ComparisonResult createResultWithCode(final ComparisonResult.Code code) {
		return new ComparisonResult(code, 33, true, new BlockHeight(66));
	}
}
