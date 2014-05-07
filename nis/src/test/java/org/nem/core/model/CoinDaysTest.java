package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CoinDaysTest {
	@Test
	public void coinDaysBalancesAreCalculatedCorrectly() {
		// Arrange:
		final CoinDays coinDays = new CoinDays();

		// Act:
		coinDays.addCoinDay(createCoinDay(1, 1337));

		// Assert:
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1440)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(0)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(1337)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(13_370_000)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(14401)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(133_700_000)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(144001)).getWeightedAmount(), IsEqual.equalTo(Amount.fromNem(1337)));
	}

	@Test
	public void coinDaysLateBlock() {
		// Arrange:
		final CoinDays coinDays = new CoinDays();

		// Act:
		coinDays.addCoinDay(createCoinDay(14400, 1337));

		// Assert:
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(14400)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(0)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(14401)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(1337)));
	}

	@Test
	public void coinDaysFromSingleDayAreGroupedCorrectly() {
		// Arrange:
		final CoinDays coinDays = new CoinDays();

		for (long i = 1; i <= 1440; ++i) {
			coinDays.addCoinDay(createCoinDay(i, 1));
		}
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(1440)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(14_400_000)));
	}

	@Test
	public void coinDaysFromTwoDaysAreGroupedCorrectly() {
		// Arrange:
		final CoinDays coinDays = new CoinDays();

		for (long i = 1; i <= 2880; ++i) {
			coinDays.addCoinDay(createCoinDay(i, 1));
		}
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(1440)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(2880)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(1440)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(2881)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(2880)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(14_400_000)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(2880)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(14_400_000)));
		// (1440*2 + 1440*1) / 100
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(2881)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(43_200_000)));
	}

	@Test
	public void balanceForCoinDaysAcrossHundredDaysIsCalculatedCorrectly() {
		final CoinDays coinDays = new CoinDays();

		for (long i = 0; i < 100; ++i) {
			coinDays.addCoinDay(createCoinDay(1+1440*i, 100-i));
		}

		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(100)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(144000)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(5049)));
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(144001)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(5050)));

		// sum(map(lambda a: a*a, range(1,101)))*1000000/100
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(144001)).getWeightedAmount(), IsEqual.equalTo(Amount.fromMicroNem(3383500000L)));
	}

	@Test
	public void canRevertCoinDay() {
		// Arrange:
		final CoinDays coinDays = new CoinDays();
		coinDays.addCoinDay(createCoinDay(1, 1337));

		// Act:
		coinDays.revertCoinDay(createCoinDay(1, 7));

		// Assert:
		Assert.assertThat(coinDays.getCoinDayWeightedBalance(new BlockHeight(1441)).getUnweightedAmount(), IsEqual.equalTo(Amount.fromNem(1330)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void revertNonexistingCoinDayThrowsAnError() {
		// Arrange:
		final CoinDays coinDays = new CoinDays();

		// Act:
		coinDays.revertCoinDay(createCoinDay(1, 7));
	}

	private CoinDay createCoinDay(long height, long amount) {
		return  new CoinDay(new BlockHeight(height), Amount.fromNem(amount));
	}
}
