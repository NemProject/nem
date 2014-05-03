package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class CoinDayTest {
	private static CoinDay createCoinDay(long blockHeight, long amount) {
		return new CoinDay(new BlockHeight(blockHeight), Amount.fromNem(amount));
	}

	//region constructor
	@Test
	public void ctorSetsFields() {
		// Arrange:
		final CoinDay coinDay = createCoinDay(10L, 1000L);

		// Assert:
		Assert.assertThat(coinDay.getHeight(), IsEqual.equalTo(new BlockHeight(10L)));
		Assert.assertThat(coinDay.getAmount(), IsEqual.equalTo(Amount.fromNem(1000L)));
	}
	//endregion

	// region addAmount
	@Test
	public void amountCanBeAddedToCoinDay() {
		// Arrange:
		final CoinDay coinDay = createCoinDay(10L, 1000L);
		final Amount amount = Amount.fromNem(1111L);

		// Act:
		coinDay.addAmount(amount);

		// Assert:
		Assert.assertThat(coinDay.getAmount(), IsEqual.equalTo(Amount.fromNem(2111L)));
	}
	// endregion addAmount
}
