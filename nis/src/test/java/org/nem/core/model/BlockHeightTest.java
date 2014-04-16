package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

import java.security.InvalidParameterException;

public class BlockHeightTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(BlockHeight.ONE, IsEqual.equalTo(new BlockHeight(1)));
	}

	//endregion

	//region constructor

	@Test(expected = InvalidParameterException.class)
	public void cannotBeCreatedAroundNegativeHeight() {
		// Act:
		new BlockHeight(-1);
	}

	@Test(expected = InvalidParameterException.class)
	public void cannotBeCreatedAroundZeroHeight() {
		// Act:
		new BlockHeight(0);
	}

	@Test
	public void canBeCreatedAroundPositiveHeight() {
		// Act:
		final BlockHeight height = new BlockHeight(1);

		// Assert:
		Assert.assertThat(height.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region next

	@Test
	public void nextHeightIsOneGreaterThanCurrentHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(45);

		// Act:
		final BlockHeight nextHeight = height.next();

		// Assert:
		Assert.assertThat(nextHeight, IsNot.not(IsEqual.equalTo(height)));
		Assert.assertThat(nextHeight, IsEqual.equalTo(new BlockHeight(46)));
	}

	//endregion

	//region subtract

	@Test
	public void heightsCanBeSubtracted() {
		// Arrange:
		final BlockHeight height1 = new BlockHeight(17);
		final BlockHeight height2 = new BlockHeight(3);

		// Assert:
		Assert.assertThat(14L, IsEqual.equalTo(height1.subtract(height2)));
		Assert.assertThat(-14L, IsEqual.equalTo(height2.subtract(height1)));
	}

	//endregion
}
