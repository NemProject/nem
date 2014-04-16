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

	//region compareTo

	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final BlockHeight height1 = new BlockHeight(7);
		final BlockHeight height2 = new BlockHeight(7);

		// Assert:
		Assert.assertThat(height1.compareTo(height2), IsEqual.equalTo(0));
		Assert.assertThat(height2.compareTo(height1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final BlockHeight height1 = new BlockHeight(7);
		final BlockHeight height2 = new BlockHeight(8);

		// Assert:
		Assert.assertThat(height1.compareTo(height2), IsEqual.equalTo(-1));
		Assert.assertThat(height2.compareTo(height1), IsEqual.equalTo(1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final BlockHeight height = new BlockHeight(7);

		// Assert:
		Assert.assertThat(new BlockHeight(7), IsEqual.equalTo(height));
		Assert.assertThat(new BlockHeight(6), IsNot.not(IsEqual.equalTo(height)));
		Assert.assertThat(new BlockHeight(8), IsNot.not(IsEqual.equalTo(height)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(height)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)height)));
	}

	@Test
	public void hashCodesAreOnlyEqualForEquivalentObjects() {
		// Arrange:
		final BlockHeight height = new BlockHeight(7);
		final int hashCode = height.hashCode();

		// Assert:
		Assert.assertThat(new BlockHeight(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new BlockHeight(6).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new BlockHeight(8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsRawBlockHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(22561);

		// Assert:
		Assert.assertThat(height.toString(), IsEqual.equalTo("22561"));
	}

	//endregion
}
