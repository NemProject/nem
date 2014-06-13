package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;

public class HistoricalBalanceTest {

	//region constructor
	@Test
	public void historicalBalanceCanBeConstructedWithInitialValues() {
		// Arrange:
		final HistoricalBalance balance1 = new HistoricalBalance(new BlockHeight(10L), new Amount(1000L));

		// Assert:
		Assert.assertThat(balance1.getHeight().getRaw(), IsEqual.equalTo(10L));
		Assert.assertThat(balance1.getBalance().getNumMicroNem(), IsEqual.equalTo(1000L));
	}
	//endregion

	//region add / subtract
	@Test
	public void amountCanBeAddedToHistoricalBalance() {
		// Arrange:
		final HistoricalBalance balance = createTestHistoricalBalance(20L, 2000L);
		final Amount amount = new Amount(111);

		// Act:
		balance.add(amount);

		// Assert:
		Assert.assertThat(balance.getBalance().getNumMicroNem(), IsEqual.equalTo(2111L));
	}

	@Test
	public void amountCanBeSubtractedFromHistoricalBalanceWithLargerAmount() {
		// Arrange:
		final HistoricalBalance balance = createTestHistoricalBalance(20L, 2000L);
		final Amount amount = new Amount(200);

		// Act:
		balance.subtract(amount);

		// Assert:
		Assert.assertThat(balance.getBalance().getNumMicroNem(), IsEqual.equalTo(1800L));
	}
	//endregion

	@Test(expected = IllegalArgumentException.class)
	public void amountCannotBeSubtractedFromHistoricalBalanceWithSmallerAmount() {
		// Arrange:
		final HistoricalBalance balance = createTestHistoricalBalance(20L, 200L);
		final Amount amount = new Amount(2000);

		// Act:
		balance.subtract(amount);
	}
	
	//region compareTo
	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final HistoricalBalance balance1 = createTestHistoricalBalance(20L, 0L);
		final HistoricalBalance balance2 = createTestHistoricalBalance(20L, 0L);

		// Assert:
		Assert.assertThat(balance1.compareTo(balance2), IsEqual.equalTo(0));
		Assert.assertThat(balance2.compareTo(balance1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final HistoricalBalance balance1 = createTestHistoricalBalance(20L, 0L);
		final HistoricalBalance balance2 = createTestHistoricalBalance(21L, 0L);

		// Assert:
		Assert.assertThat(balance1.compareTo(balance2), IsEqual.equalTo(-1));
		Assert.assertThat(balance2.compareTo(balance1), IsEqual.equalTo(1));
	}
	//endregion

	private HistoricalBalance createTestHistoricalBalance(long height, long amount) {
		return new HistoricalBalance(new BlockHeight(height), new Amount(amount));
	}
}
