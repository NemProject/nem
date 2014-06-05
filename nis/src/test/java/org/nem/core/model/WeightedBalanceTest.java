package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;

public class WeightedBalanceTest {

	private static final int ESTIMATED_BLOCKS_PER_DAY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

	//region ctor
	@Test
	public void vestedBalanceCanBeConstructed() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.ZERO.createReceive(new BlockHeight(120), Amount.fromNem(1_000_000));

		// Assert
		assertWeightedBalance(weightedBalance, 120, Amount.ZERO, Amount.fromNem(1_000_000));
	}
	//endregion ctor

	//region add/sub

	@Test
	public void canReceiveToVestedBalance() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		final WeightedBalance result = weightedBalance.createReceive(BlockHeight.ONE, Amount.fromNem(100_000));

		// Assert:
		assertWeightedBalance(result, 1, Amount.ZERO, Amount.fromNem(1_100_000));
	}

	@Test
	public void canReceiveToVestedBalanceIfPartiallyVested() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromNem(1_000_000));
		final WeightedBalance result1 = advanceDays(weightedBalance, 10);
		final WeightedBalance result2 = result1.createSend(result1.getBlockHeight(), Amount.fromNem(100_000));

		// Act
		final WeightedBalance result3 = result2.createReceive(result2.getBlockHeight(), Amount.fromNem(100_000));

		// Assert
		Assert.assertThat(result3.getVestedBalance().getNumMicroNem(), IsEqual.equalTo(164_634_473_802L));
		Assert.assertThat(result3.getUnvestedBalance().getNumMicroNem(), IsEqual.equalTo(835_365_526_198L));
	}

//	 WTH?
//	@Test
//	public void canUndoReceiveToVestedBalanceIfPartiallyVested() {
//		// Arrange:
//		WeightedBalance weightedBalance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromNem(1_000_000));
//		weightedBalance = advanceDays(weightedBalance, 10);
//		weightedBalance.send(Amount.fromNem(100_000));
//
//		// Act (at this point gcd(balance, vested+unvested) != balance):
//		weightedBalance.undoReceive(Amount.fromNem(100_000));
//
//		// Assert (ratio vested/unvested should be 2.7416293421830850665769733133716):
//		Assert.assertThat(weightedBalance.getVestedBalance().getNumMicroNem(), IsEqual.equalTo(586_189_403_910L));
//		Assert.assertThat(weightedBalance.getUnvestedBalance().getNumMicroNem(), IsEqual.equalTo(213_810_596_090L));
//	}

	//endregion

	//region next/prev
	@Test
	public void vestedBalanceCanBeIterated() {
		// Arrange:
		final WeightedBalance initialBalance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		final WeightedBalance weightedBalance = initialBalance.next();

		// Assert:
		assertWeightedBalance(
				weightedBalance,
				1441,
				Amount.fromMicroNem(20),
				Amount.fromMicroNem(980));
	}

	@Test
	public void vestedBalanceAfter50Days() {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		final WeightedBalance result = advanceDays(weightedBalance, 50);

		// Assert:
		assertWeightedBalance(
				result,
				50*1440+1,
				Amount.fromMicroNem(652),
				Amount.fromMicroNem(348));
	}

	@Test
	public void amountsAreNotLostDuringIterationOfTinyAmounts() {
		// Arrange:
		final WeightedBalance balance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromMicroNem(75));

		WeightedBalance result = balance;
		for (int i = 1; i < 75; ++i) {
			// Act
			result = result.next();

			// Assert:
			Assert.assertThat(result.getBlockHeight(), IsEqual.equalTo(new BlockHeight(1440*i + 1)));
			final Amount sum = result.getVestedBalance().add(result.getUnvestedBalance());
			Assert.assertThat(sum, IsEqual.equalTo(Amount.fromMicroNem(75)));
		}
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

		final WeightedBalance result = weightedBalance.createSend(weightedBalance.getBlockHeight(), Amount.fromMicroNem(2_000));

		final Amount vested2 = result.getVestedBalance();
		double ratio2 = calculateRatio(result);

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

	private static WeightedBalance prepareVestedForSending(long amount) {
		// Arrange:
		final WeightedBalance weightedBalance = WeightedBalance.ZERO.createReceive(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act:
		return advanceDays(weightedBalance, 30);
	}
	//endregion

	@Test
	public void vestedBalanceCanBeCompared() {
		// Arrange:
		final WeightedBalance weightedBalance1 = WeightedBalance.ZERO.
				createReceive(new BlockHeight(10), Amount.fromMicroNem(1000));
		final WeightedBalance weightedBalance2 = WeightedBalance.ZERO.createReceive(new BlockHeight(20), Amount.fromMicroNem(1000));

		// Assert:
		Assert.assertThat(weightedBalance1.compareTo(weightedBalance1), IsEqual.equalTo(0));
		Assert.assertThat(weightedBalance1.compareTo(weightedBalance2), IsEqual.equalTo(-1));
		Assert.assertThat(weightedBalance2.compareTo(weightedBalance1), IsEqual.equalTo(1));
	}

	@Test
	public void multipleReceiveTest() {
		// Arrange:
		final WeightedBalance balance = WeightedBalance.ZERO.createReceive(new BlockHeight(1440), Amount.fromNem(123));

		// Act / Assert:
		final WeightedBalance result1 = balance.next();
		assertWeightedBalance(result1, 1441, Amount.fromMicroNem(2_460_000L), Amount.fromMicroNem(120_540_000L));

		final WeightedBalance result2 = result1.next();
		assertWeightedBalance(result2, 2881, Amount.fromMicroNem(4_870_800L), Amount.fromMicroNem(118_129_200L));

		// Assert: receive does not change vested balance
		final WeightedBalance result3 = result2.createReceive(result2.getBlockHeight(), Amount.fromNem(345));
		assertWeightedBalance(result3, 2881, Amount.fromMicroNem(4_870_800L), Amount.fromMicroNem(463_129_200));
	}

	private static WeightedBalance advanceDays(final WeightedBalance weightedBalance, int numDays) {
		WeightedBalance result = weightedBalance;
		for (int i = 0; i < numDays; ++i)
			result = result.next();

		return result;
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
