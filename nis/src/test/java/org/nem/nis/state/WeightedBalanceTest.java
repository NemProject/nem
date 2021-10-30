package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;

public class WeightedBalanceTest {

	// region constants / create*

	@Test
	public void weightedBalanceZeroIsInitializedCorrectly() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.ZERO;

		// Assert:
		assertWeightedBalance(weightedBalance, 1, Amount.ZERO, Amount.ZERO);
		MatcherAssert.assertThat(weightedBalance.getAmount(), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void fullyUnvestedBalanceCanBeCreated() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.createUnvested(new BlockHeight(120), Amount.fromNem(1_000_000));

		// Assert:
		assertWeightedBalance(weightedBalance, 120, Amount.ZERO, Amount.fromNem(1_000_000));
		MatcherAssert.assertThat(weightedBalance.getAmount(), IsEqual.equalTo(Amount.fromNem(1_000_000)));
	}

	@Test
	public void fullyVestedBalanceCanBeCreated() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.createVested(new BlockHeight(120), Amount.fromNem(1_000_000));

		// Assert:
		assertWeightedBalance(weightedBalance, 120, Amount.fromNem(1_000_000), Amount.ZERO);
		MatcherAssert.assertThat(weightedBalance.getAmount(), IsEqual.equalTo(Amount.fromNem(1_000_000)));
	}

	@Test
	public void partiallyVestedAndUnvestedBalanceCanBeCreated() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.create(new BlockHeight(120), Amount.fromNem(1_000_000),
				Amount.fromNem(2_000_000));

		// Assert:
		assertWeightedBalance(weightedBalance, 120, Amount.fromNem(1_000_000), Amount.fromNem(2_000_000));
		MatcherAssert.assertThat(weightedBalance.getAmount(), IsEqual.equalTo(Amount.fromNem(3_000_000)));
	}

	// endregion ctor

	// region receive

	@Test
	public void canReceiveIfBalanceIsFullyUnvested() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		final WeightedBalance result = original.createReceive(BlockHeight.ONE, Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.ZERO, Amount.fromNem(1_100_000));
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(100_000)));
	}

	@Test
	public void canReceiveIfBalanceIsPartiallyVested() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromNem(1_000_000));
		final WeightedBalance balance2 = advanceDays(original, 10); // UV[2] ~ 1M *(.9^10)

		// Act:
		final WeightedBalance balance3 = balance2.createSend(balance2.getBlockHeight(), Amount.fromNem(100_000)); // UV[3] ~ UV[2] * .9
		final WeightedBalance result = balance3.createReceive(balance3.getBlockHeight(), Amount.fromNem(100_000)); // UV[4] ~ UV[3] + 100000

		// Assert:
		assertWeightedBalance(result, 14401, Amount.fromMicroNem(586_189_403_910L), Amount.fromMicroNem(413_810_596_090L));
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(100_000)));
	}

	@Test
	public void canReceiveIfBalanceIsFullyVested() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.createVested(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		final WeightedBalance result = original.createReceive(BlockHeight.ONE, Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.fromNem(1_000_000), Amount.fromNem(100_000));
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(100_000)));
	}

	@Test
	public void multipleAmountsCanBeReceivedOverTime() {
		// Arrange:
		final WeightedBalance balance = WeightedBalance.createUnvested(new BlockHeight(1440), Amount.fromNem(123));

		// Act / Assert:
		final WeightedBalance result1 = balance.next(); // UV = 123 * .9
		assertWeightedBalance(result1, 1441, Amount.fromMicroNem(12_300_000L), Amount.fromMicroNem(110_700_000L));

		final WeightedBalance result2 = result1.next(); // UV = 123 * .9^2
		assertWeightedBalance(result2, 2881, Amount.fromMicroNem(23_370_000L), Amount.fromMicroNem(99_630_000L));

		// Assert: receive does not change vested balance
		final WeightedBalance result3 = result2.createReceive(result2.getBlockHeight(), Amount.fromNem(345)); // UV = 123 * .9^2 + 345
		assertWeightedBalance(result3, 2881, Amount.fromMicroNem(23_370_000L), Amount.fromMicroNem(444_630_000L));
	}

	// endregion

	// region next

	@Test
	public void balanceCanBeAdvancedOneDay() {
		// Arrange:
		final WeightedBalance initialBalance = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act:
		final WeightedBalance weightedBalance = initialBalance.next();

		// Assert:
		assertWeightedBalance(weightedBalance, 1441, Amount.fromMicroNem(100), Amount.fromMicroNem(900));
		MatcherAssert.assertThat(weightedBalance.getAmount(), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void balanceIsAdvancedToDayBoundary() {
		// Arrange:
		final WeightedBalance initialBalance = WeightedBalance.createUnvested(new BlockHeight(770), Amount.fromMicroNem(1000));

		// Act:
		final WeightedBalance weightedBalance = initialBalance.next();

		// Assert:
		assertWeightedBalance(weightedBalance, 1441, Amount.fromMicroNem(100), Amount.fromMicroNem(900));
		MatcherAssert.assertThat(weightedBalance.getAmount(), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void balanceCanBeAdvancedFiftyDays() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromMicroNem(1000000));

		// Act:
		final WeightedBalance result = advanceDays(weightedBalance, 50);

		// Assert:
		assertWeightedBalance(result, 50 * 1440 + 1, Amount.fromMicroNem(994_851), Amount.fromMicroNem(5149)); // ~ 1000 * .9 ^ 50
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void amountsAreNotLostDuringAdvancementOfTinyAmounts() {
		// Arrange:
		WeightedBalance balance = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromMicroNem(75));
		for (int i = 1; i < 75; ++i) {
			// Act:
			balance = balance.next();

			// Assert:
			MatcherAssert.assertThat(balance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(1440 * i + 1)));
			final Amount sum = balance.getVestedBalance().add(balance.getUnvestedBalance());
			MatcherAssert.assertThat(sum, IsEqual.equalTo(Amount.fromMicroNem(75)));
		}
	}

	// endregion

	// region send

	@Test
	public void canSendIfBalanceIsFullyUnvested() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		final WeightedBalance result = original.createSend(BlockHeight.ONE, Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.ZERO, Amount.fromNem(900_000));
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(100_000)));
	}

	@Test
	public void canSendIfBalanceIsPartiallyVested() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromNem(1_000_000));
		final WeightedBalance balance2 = advanceDays(original, 10); // UV[2] ~ 1M *(.9^10)

		// Act:
		final WeightedBalance result = balance2.createSend(balance2.getBlockHeight(), Amount.fromNem(100_000)); // UV[3] ~ UV[2] * .9

		// Assert:
		assertWeightedBalance(result, 14401, Amount.fromMicroNem(586_189_403_910L), Amount.fromMicroNem(313_810_596_090L));
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(100_000)));
	}

	@Test
	public void canSendIfBalanceIsFullyVested() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.createVested(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		final WeightedBalance result = original.createSend(BlockHeight.ONE, Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.fromNem(900_000), Amount.ZERO);
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(100_000)));
	}

	@Test
	public void canSendIfAmountIsEqualToBalance() {
		// Arrange:
		WeightedBalance original = WeightedBalance.createVested(BlockHeight.ONE, Amount.fromNem(1_000_000));
		original = advanceDays(original, 7);

		// Act:
		final WeightedBalance result = original.createSend(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.ZERO, Amount.ZERO);
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromNem(1_000_000)));
	}

	@Test
	public void createSendAddsVestedToUnvestedAndSetsVestedToZeroIfCalculatedNewVestedAmountIsNegative() {
		// Arrange:
		final WeightedBalance original = WeightedBalance.create(BlockHeight.ONE, Amount.fromMicroNem(2549716), Amount.fromMicroNem(450284));

		// Act:
		// ratio = 0.15009466666666665, sendUv = 450283.99999999995 --> 450283, vested hitting negative value of -1
		final WeightedBalance result = original.createSend(BlockHeight.ONE, Amount.fromMicroNem(3_000_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.ZERO, Amount.ZERO);
		MatcherAssert.assertThat(result.getAmount(), IsEqual.equalTo(Amount.fromMicroNem(3_000_000)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void cannotSendIfAmountIsLargerThanBalance() {
		// Arrange:
		WeightedBalance original = WeightedBalance.createVested(BlockHeight.ONE, Amount.fromNem(1_000_000));
		original = advanceDays(original, 7);

		// Act:
		original.createSend(BlockHeight.ONE, Amount.fromNem(1_000_001));
	}

	@Test
	public void sendingKeepsRatio() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.createVested(BlockHeight.ONE, Amount.fromNem(10_000));

		// Act:
		final Amount vested1 = weightedBalance.getVestedBalance();
		final double ratio1 = calculateRatio(weightedBalance);

		final WeightedBalance result = weightedBalance.createSend(weightedBalance.getBlockHeight(), Amount.fromMicroNem(2_000));

		final Amount vested2 = result.getVestedBalance();
		final double ratio2 = calculateRatio(result);

		// Assert:
		Assert.assertTrue((ratio1 - ratio2) <= 0.0001);
		Assert.assertTrue(vested1.getNumMicroNem() - Math.ceil(ratio1 * 2_000) <= vested2.getNumMicroNem());
		Assert.assertTrue(vested1.getNumMicroNem() - Math.floor(ratio1 * 2_000) >= vested2.getNumMicroNem());
	}

	private static double calculateRatio(final WeightedBalance balance) {
		final long vestedBalance = balance.getVestedBalance().getNumMicroNem();
		final long unvestedBalance = balance.getUnvestedBalance().getNumMicroNem();
		return vestedBalance / (0d + vestedBalance + unvestedBalance);
	}

	// endregion

	// region compare / copy

	@Test
	public void balanceCanBeCompared() {
		// Arrange:
		final WeightedBalance weightedBalance1 = WeightedBalance.createUnvested(new BlockHeight(10), Amount.fromMicroNem(1000));
		final WeightedBalance weightedBalance2 = WeightedBalance.createUnvested(new BlockHeight(20), Amount.fromMicroNem(1000));

		// Assert:
		MatcherAssert.assertThat(weightedBalance1.compareTo(weightedBalance1), IsEqual.equalTo(0));
		MatcherAssert.assertThat(weightedBalance1.compareTo(weightedBalance2), IsEqual.equalTo(-1));
		MatcherAssert.assertThat(weightedBalance2.compareTo(weightedBalance1), IsEqual.equalTo(1));
	}

	@Test
	public void canCopyWeightedBalance() {
		// Arrange:
		final WeightedBalance originalBalance = WeightedBalance.createUnvested(BlockHeight.ONE, Amount.fromMicroNem(1000)).next();

		// Act
		final WeightedBalance copiedBalance = originalBalance.copy();

		// Assert:
		assertWeightedBalance(copiedBalance, 1441, Amount.fromMicroNem(100), Amount.fromMicroNem(900));
		MatcherAssert.assertThat(copiedBalance.getAmount(), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void canCopyWeightedBalanceWithAmount() {
		// Arrange:
		final WeightedBalance originalBalance = WeightedBalance.create(new BlockHeight(3), Amount.fromNem(17), Amount.fromNem(12));

		// Act
		final WeightedBalance copiedBalance = originalBalance.copy();

		// Assert:
		assertWeightedBalance(copiedBalance, 3, Amount.fromNem(17), Amount.fromNem(12));
		MatcherAssert.assertThat(copiedBalance.getAmount(), IsEqual.equalTo(Amount.fromNem(29)));
	}

	// endregion

	private static WeightedBalance advanceDays(final WeightedBalance weightedBalance, final int numDays) {
		WeightedBalance result = weightedBalance;
		for (int i = 0; i < numDays; ++i) {
			result = result.next();
		}

		return result;
	}

	private static void assertWeightedBalance(final WeightedBalance balance, final int blockHeight, final Amount vestedAmount,
			final Amount unvestedAmount) {
		// Assert
		MatcherAssert.assertThat(balance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(blockHeight)));
		MatcherAssert.assertThat(balance.getVestedBalance(), IsEqual.equalTo(vestedAmount));
		MatcherAssert.assertThat(balance.getUnvestedBalance(), IsEqual.equalTo(unvestedAmount));
		final Amount expectedBalance = vestedAmount.add(unvestedAmount);
		MatcherAssert.assertThat(balance.getBalance(), IsEqual.equalTo(expectedBalance));
	}
}
