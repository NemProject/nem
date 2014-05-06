package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for CoinDay class.
 */
public class CoinDayTest {
	// region constructor
	@Test
	public void coinDayCanBeConstructedWithInitialValues() {
		// Arrange:
		final CoinDay coinDay1 = new CoinDay(
				new BlockHeight(10L), new Amount(1000L));

		// Assert:
		Assert.assertThat(coinDay1.getHeight().getRaw(), IsEqual.equalTo(10L));
		Assert.assertThat(coinDay1.getBalance().getNumMicroNem(),
				IsEqual.equalTo(1000L));
	}

	// endregion

	// region add / subtract
	@Test
	public void amountCanBeAddedToCoinDay() {
		// Arrange:
		final CoinDay coinDay = createTestCoinDay(20L,
				2000L);
		final Amount amount = new Amount(111);

		// Act:
		coinDay.add(amount);

		// Assert:
		Assert.assertThat(coinDay.getBalance().getNumMicroNem(),
				IsEqual.equalTo(2111L));
	}

	@Test
	public void amountCanBeSubtractedFromCoinDayWithLargerAmount() {
		// Arrange:
		final CoinDay coinDay = createTestCoinDay(20L,
				2000L);
		final Amount amount = new Amount(200);

		// Act:
		coinDay.subtract(amount);

		// Assert:
		Assert.assertThat(coinDay.getBalance().getNumMicroNem(),
				IsEqual.equalTo(1800L));
	}

	// endregion

	@Test(expected = IllegalArgumentException.class)
	public void amountCannotBeSubtractedFromCoinDayWithSmallerAmount() {
		// Arrange:
		final CoinDay coinDay = createTestCoinDay(20L, 200L);
		final Amount amount = new Amount(2000);

		// Act:
		coinDay.subtract(amount);
	}

	// region compareTo
	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final CoinDay coinDay1 = createTestCoinDay(20L, 0L);
		final CoinDay coinDay2 = createTestCoinDay(20L, 0L);

		// Assert:
		Assert.assertThat(coinDay1.compareTo(coinDay2), IsEqual.equalTo(0));
		Assert.assertThat(coinDay2.compareTo(coinDay1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final CoinDay coinDay1 = createTestCoinDay(20L, 0L);
		final CoinDay coinDay2 = createTestCoinDay(21L, 0L);

		// Assert:
		Assert.assertThat(coinDay1.compareTo(coinDay2), IsEqual.equalTo(-1));
		Assert.assertThat(coinDay2.compareTo(coinDay1), IsEqual.equalTo(1));
	}

	// endregion

	private CoinDay createTestCoinDay(long height,
			long amount) {
		return new CoinDay(new BlockHeight(height),
				new Amount(amount));
	}
}
