package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

public class WeightedBalanceTest {

	private static final int ESTIMATED_BLOCKS_PER_DAY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

	@Test
	public void vestedBalanceCanBeConstructed() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(new BlockHeight(120), Amount.fromNem(1_000_000));

		// Assert
		assertWeightedBalance(weightedBalance, 120, Amount.ZERO, Amount.fromNem(1_000_000));
	}

	//region add/sub

	@Test
	public void canReceiveToVestedBalance() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		weightedBalance.receive(Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(weightedBalance, 1, Amount.ZERO, Amount.fromNem(1_100_000));
	}

	@Test
	public void canReceiveToVestedBalanceIfPartiallyVested() {
		// Arrange:
		WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));
		weightedBalance = advanceDays(weightedBalance, 10);
		weightedBalance.send(Amount.fromNem(100_000));

		// Act (at this point gcd(balance, vested+unvested) != balance):
		weightedBalance.receive(Amount.fromNem(100_000));

		// Assert (ratio vested/unvested should be 1.4165645090985277578081458837204):
		Assert.assertThat(weightedBalance.getVestedBalance().getNumMicroNem(), IsEqual.equalTo(586_189_403_910L));
		Assert.assertThat(weightedBalance.getUnvestedBalance().getNumMicroNem(), IsEqual.equalTo(413_810_596_090L));
	}

	@Test
	public void canUndoReceiveFromVestedBalance() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		weightedBalance.undoReceive(Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(weightedBalance, 1, Amount.ZERO, Amount.fromNem(900_000));
	}

	@Test(expected = IllegalArgumentException.class)
	public void undoReceiveMightThrow() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(100_000));

		// Act:
		weightedBalance.undoReceive(Amount.fromNem(100_001));
	}

	//endregion

	//region next/prev

	@Test
	public void vestedBalanceCanBeIterated() {
		// Arrange:
		final WeightedBalance initialBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		final WeightedBalance weightedBalance = initialBalance.next();

		// Assert:
		assertWeightedBalance(
				weightedBalance,
				ESTIMATED_BLOCKS_PER_DAY + 1,
				Amount.fromMicroNem(100),
				Amount.fromMicroNem(900));
	}

	@Test
	public void vestedBalanceAfter50Days() {
		// Arrange:
		WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		weightedBalance = advanceDays(weightedBalance, 50);

		// Assert:
		assertWeightedBalance(
				weightedBalance,
				ESTIMATED_BLOCKS_PER_DAY * 50 + 1,
				Amount.fromMicroNem(994),
				Amount.fromMicroNem(6));
	}

	@Test
	public void amountsAreNotLostDuringIterationOfTinyAmounts() {
		// Arrange:
		WeightedBalance balance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(75));

		for (int i = 1; i < 75; ++i) {
			// Act
			balance = balance.next();

			// Assert:
			Assert.assertThat(balance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(ESTIMATED_BLOCKS_PER_DAY * i + 1)));
			final Amount sum = balance.getVestedBalance().add(balance.getUnvestedBalance());
			Assert.assertThat(sum, IsEqual.equalTo(Amount.fromMicroNem(75)));
		}
	}

	@Test
	public void amountsAreProperlyRestored() {
		for (long i = 1; i < 1000; ++i) {
			singleTest(i);
		}
	}

	private static void singleTest(long amount) {
		// Arrange:
		final WeightedBalance initialBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act
		final WeightedBalance roundTrippedBalance = initialBalance.next().previous();

		// Assert:
		assertWeightedBalance(roundTrippedBalance, 1, Amount.ZERO, Amount.fromMicroNem(amount));
	}

	//endregion

	//region send/undoSend

	@Test
	public void sendingKeepsRatio() {
		// Arrange:
		final WeightedBalance weightedBalance = prepareVestedForSending(10_000);

		// Act:
		final Amount vested1 = weightedBalance.getVestedBalance();
		double ratio1 = calculateRatio(weightedBalance);

		weightedBalance.send(Amount.fromMicroNem(2_000));

		final Amount vested2 = weightedBalance.getVestedBalance();
		double ratio2 = calculateRatio(weightedBalance);

		// Assert:
		Assert.assertTrue((ratio1 - ratio2) <= 0.0001);
		Assert.assertTrue(vested1.getNumMicroNem() - Math.ceil(ratio1 * 2_000) <= vested2.getNumMicroNem());
		Assert.assertTrue(vested1.getNumMicroNem() - Math.floor(ratio1 * 2_000) >= vested2.getNumMicroNem());
	}

	private static double calculateRatio(final WeightedBalance balance) {
		long vestedBalance = balance.getVestedBalance().getNumMicroNem();
		long unvestedBalance = balance.getUnvestedBalance().getNumMicroNem();
		return vestedBalance / (0d + vestedBalance + unvestedBalance);
	}

	@Test
	public void undoSendRestoresValues() {
		// Arrange:
		final WeightedBalance weightedBalance = prepareVestedForSending(100_000);

		// Act:
		final Amount vested1 = weightedBalance.getVestedBalance();
		final Amount unvested1 = weightedBalance.getUnvestedBalance();
		weightedBalance.send(Amount.fromMicroNem(20_000));
		weightedBalance.undoSend(Amount.fromMicroNem(20_000));
		final Amount vested2 = weightedBalance.getVestedBalance();
		final Amount unvested2 = weightedBalance.getUnvestedBalance();

		// Assert:
		Assert.assertThat(vested2, IsEqual.equalTo(vested1));
		Assert.assertThat(unvested2, IsEqual.equalTo(unvested1));
	}

	@Test
	public void undoSendRestoresValuesMultipleSends() {
		// Arrange:
		for (int i = 1; i <= 10; ++i) {
			final WeightedBalance weightedBalance = prepareVestedForSending(1_000_000);

			// Act:
			final Amount vested1 = weightedBalance.getVestedBalance();
			final Amount unvested1 = weightedBalance.getUnvestedBalance();
			for (int j = 0; j < i; ++j) {
				weightedBalance.send(Amount.fromMicroNem(20_000 + j));
			}
			for (int j = 0; j < i; ++j) {
				weightedBalance.undoSend(Amount.fromMicroNem(20_000 + (i - j - 1)));
			}
			final Amount vested2 = weightedBalance.getVestedBalance();
			final Amount unvested2 = weightedBalance.getUnvestedBalance();

			// Assert:
			Assert.assertThat(vested2, IsEqual.equalTo(vested1));
			Assert.assertThat(unvested2, IsEqual.equalTo(unvested1));
		}
	}

	private static WeightedBalance prepareVestedForSending(long amount) {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act:
		return advanceDays(weightedBalance, 30);
	}

	//endregion

	@Test
	public void vestedBalanceCanBeCompared() {
		// Arrange:
		final WeightedBalance weightedBalance1 = new WeightedBalance(new BlockHeight(10), Amount.fromMicroNem(1000));
		final WeightedBalance weightedBalance2 = new WeightedBalance(new BlockHeight(20), Amount.fromMicroNem(1000));

		// Assert:
		Assert.assertThat(weightedBalance1.compareTo(weightedBalance1), IsEqual.equalTo(0));
		Assert.assertThat(weightedBalance1.compareTo(weightedBalance2), IsEqual.equalTo(-1));
		Assert.assertThat(weightedBalance2.compareTo(weightedBalance1), IsEqual.equalTo(1));
	}

	@Test
	public void multipleReceiveTest() {
		// Arrange:
		WeightedBalance balance = new WeightedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act / Assert:
		balance = balance.next();
		assertWeightedBalance(balance, 2880, Amount.fromMicroNem(12_300_000), Amount.fromMicroNem(110_700_000));

		balance = balance.next();
		assertWeightedBalance(balance, 4320, Amount.fromMicroNem(23_370_000), Amount.fromMicroNem(99_630_000));

		// Assert: receive does not change vested balance
		balance.receive(Amount.fromNem(345));
		assertWeightedBalance(balance, 4320, Amount.fromMicroNem(23_370_000), Amount.fromMicroNem(444_630_000));
	}

	private static WeightedBalance advanceDays(WeightedBalance weightedBalance, int numDays) {
		for (int i = 0; i < numDays; ++i)
			weightedBalance = weightedBalance.next();

		return weightedBalance;
	}

	private static void assertWeightedBalance(
			final WeightedBalance balance,
			int blockHeight,
			final Amount vestedAmount,
			final Amount unvestedAmount) {
		// Assert
		Assert.assertThat(balance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(blockHeight)));
		Assert.assertThat(balance.getVestedBalance(), IsEqual.equalTo(vestedAmount));
		Assert.assertThat(balance.getUnvestedBalance(), IsEqual.equalTo(unvestedAmount));
	}
}
