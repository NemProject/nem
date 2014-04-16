package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

import java.security.InvalidParameterException;

public class AmountTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(Amount.ZERO, IsEqual.equalTo(new Amount(0)));
	}

	//endregion

	//region factories

	@Test
	public void canCreateAmountFromNem() {
		// Act:
		final Amount amount = Amount.fromNem(11);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(11000000L));
		Assert.assertThat(amount.getNumNem(), IsEqual.equalTo(11L));
	}

	@Test
	public void canCreateAmountFromMicroNem() {
		// Act:
		final Amount amount = Amount.fromMicroNem(11);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(11L));
		Assert.assertThat(amount.getNumNem(), IsEqual.equalTo(0L));
	}

	//endregion

	//region constructor

	@Test(expected = InvalidParameterException.class)
	public void cannotBeCreatedAroundNegativeAmount() {
		// Act:
		new Amount(-1);
	}

	@Test
	public void canBeCreatedAroundZeroAmount() {
		// Act:
		final Amount amount = new Amount(0);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveAmount() {
		// Act:
		final Amount amount = new Amount(1);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(1L));
	}

	//endregion

	//region getNumNem

	@Test
	public void getNumNemRoundsDownToTheNearestWholeNem() {
		// Assert:
		Assert.assertThat(Amount.fromMicroNem(11000000L).getNumNem(), IsEqual.equalTo(11L));
		Assert.assertThat(Amount.fromMicroNem(11000001L).getNumNem(), IsEqual.equalTo(11L));
		Assert.assertThat(Amount.fromMicroNem(11999999L).getNumNem(), IsEqual.equalTo(11L));
	}

	//endregion

	//region add / subtract

	@Test
	public void twoAmountsCanBeAddedTogether() {
		// Arrange:
		final Amount amount1 = new Amount(65);
		final Amount amount2 = new Amount(111);

		// Act:
		final Amount result1 = amount1.add(amount2);
		final Amount result2 = amount2.add(amount1);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(new Amount(176)));
		Assert.assertThat(result2, IsEqual.equalTo(new Amount(176)));
	}

	@Test
	public void smallerAmountCanBeSubtractedFromLargerAmount() {
		// Arrange:
		final Amount amount1 = new Amount(65);
		final Amount amount2 = new Amount(111);

		// Act:
		final Amount result = amount2.subtract(amount1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new Amount(46)));
	}

	@Test(expected = InvalidParameterException.class)
	public void largerAmountCannotBeSubtractedFromSmallerAmount() {
		// Arrange:
		final Amount amount1 = new Amount(65);
		final Amount amount2 = new Amount(111);

		// Act:
		amount1.subtract(amount2);
	}

	//endregion

	//region compareTo

	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final Amount amount1 = new Amount(7);
		final Amount amount2 = new Amount(7);

		// Assert:
		Assert.assertThat(amount1.compareTo(amount2), IsEqual.equalTo(0));
		Assert.assertThat(amount2.compareTo(amount1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final Amount amount1 = new Amount(7);
		final Amount amount2 = new Amount(8);

		// Assert:
		Assert.assertThat(amount1.compareTo(amount2), IsEqual.equalTo(-1));
		Assert.assertThat(amount2.compareTo(amount1), IsEqual.equalTo(1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Amount amount = new Amount(7);

		// Assert:
		Assert.assertThat(new Amount(7), IsEqual.equalTo(amount));
		Assert.assertThat(new Amount(6), IsNot.not(IsEqual.equalTo(amount)));
		Assert.assertThat(new Amount(8), IsNot.not(IsEqual.equalTo(amount)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(amount)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)amount)));
	}

	@Test
	public void hashCodesAreOnlyEqualForEquivalentObjects() {
		// Arrange:
		final Amount amount = new Amount(7);
		final int hashCode = amount.hashCode();

		// Assert:
		Assert.assertThat(new Amount(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new Amount(6).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new Amount(8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsRawAmount() {
		// Arrange:
		final Amount amount = new Amount(22561);

		// Assert:
		Assert.assertThat(amount.toString(), IsEqual.equalTo("22561"));
	}

	//endregion
}
