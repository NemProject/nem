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
	public void canAddCoinDay() {
		// Arrange:
		final CoinDays coindays = new CoinDays();
		BlockHeight height = BlockHeight.ONE;
		Amount amount = new Amount(1337);

		// Act:
		coindays.addCoinDay(new CoinDay(height, amount));

		// Assert:
		Assert.assertThat(coindays.getCoinDayWeightedBalance(height).getUnweightedAmount(), IsEqual.equalTo(new Amount(0)));
		Assert.assertThat(coindays.getCoinDayWeightedBalance(height).getWeightedAmount(), IsEqual.equalTo(amount));
	}
	
	@Test
	public void canSubtractCoinDay() {
		// Arrange:
		final CoinDays coindays = new CoinDays();
		
		// Act:
		
		// Assert:
	}
}
