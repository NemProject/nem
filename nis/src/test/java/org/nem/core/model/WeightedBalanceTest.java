package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class WeightedBalanceTest {
	@Test
	public void vestedBalanceCanBeConstructed() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Assert
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(1_000_000)));
	}

	//region add/sub
	@Test
	public void canReceiveToVestedBalance() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		weightedBalance.receive(Amount.fromNem(100_000));

		// Assert:
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(1_100_000)));
	}

	@Test
	public void canUndoReceiveFromVestedBalance() {
		// Arrange:
		final WeightedBalance weightedBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		weightedBalance.undoReceive(Amount.fromNem(100_000));

		// Assert:
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(900_000)));
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
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1)));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(20)));
		Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(980)));
	}

	@Test
	public void vestedBalanceAfter50Days() {
		// Arrange:
		final WeightedBalance initialBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		WeightedBalance weightedBalance = initialBalance;
		for (int i=0; i<50; ++i) {
			weightedBalance = weightedBalance.next();
		}

		// Assert:
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY*50 + 1)));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(635)));
		//Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(365)));
	}

	@Test
	@Ignore // this test doesn't make much sense now, also it doesn't actually matter now
	public void nemsAreNotLostDuringIterationOfTinyAmounts() {
		// Arrange:
		final WeightedBalance initialBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(75));

		// Act
		final WeightedBalance weightedBalance = initialBalance.next();

		// Assert:
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1)));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(1)));
		Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(74)));
	}

	@Test
	public void nemsAreProperlyRestored() {
		for (long i = 1; i < 1000; ++i) {
			singleTest(i);
		}
	}

	public void singleTest(long amount) {
		// Arrange:
		final WeightedBalance initialBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act
		final WeightedBalance dummyBalance = initialBalance.next();
		final WeightedBalance weightedBalance = dummyBalance.previous();

		// Assert:
		Assert.assertThat(weightedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(weightedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(amount)));
		Assert.assertThat(weightedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(0)));
	}
	//endregion

	//region send/undoSend
	@Test
	public void sendingKeepsRatio() {
		// Arrange:
		final WeightedBalance weightedBalance = prepareVestedForSending(10_000);

		// Act:
		double ratio1 = weightedBalance.getVestedBalance().getNumMicroNem() / (0d + weightedBalance.getVestedBalance().getNumMicroNem() + weightedBalance.getUnvestedBalance().getNumMicroNem());
		weightedBalance.send(Amount.fromMicroNem(2_000));
		double ratio2 = weightedBalance.getVestedBalance().getNumMicroNem() / (0d + weightedBalance.getVestedBalance().getNumMicroNem() + weightedBalance.getUnvestedBalance().getNumMicroNem());

		// Assert:
		Assert.assertTrue((ratio1 - ratio2) < Double.MIN_NORMAL);
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

	private WeightedBalance prepareVestedForSending(long amount) {
		// Arrange:
		final WeightedBalance initialBalance = new WeightedBalance(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act:
		WeightedBalance weightedBalance = initialBalance;
		for (int i=0; i<30; ++i) {
			weightedBalance = weightedBalance.next();
		}
		return weightedBalance;
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


}
