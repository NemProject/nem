package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class VestedBalanceTest {
	@Test
	public void vestedBalanceCanBeConstructed() {
		// Arrange:
		final VestedBalance vestedBalance = new VestedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Assert
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(1_000_000)));
	}

	//region add/sub
	@Test
	public void canReceiveToVestedBalance() {
		// Arrange:
		final VestedBalance vestedBalance = new VestedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		vestedBalance.receive(Amount.fromNem(100_000));

		// Assert:
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(1_100_000)));
	}

	@Test
	public void canUndoReceiveFromVestedBalance() {
		// Arrange:
		final VestedBalance vestedBalance = new VestedBalance(BlockHeight.ONE, Amount.fromNem(1_000_000));

		// Act:
		vestedBalance.undoReceive(Amount.fromNem(100_000));

		// Assert:
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromNem(900_000)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void undoReceiveMightThrow() {
		// Arrange:
		final VestedBalance vestedBalance = new VestedBalance(BlockHeight.ONE, Amount.fromNem(100_000));

		// Act:
		vestedBalance.undoReceive(Amount.fromNem(100_001));
	}
	//endregion

	//region next/prev
	@Test
	public void vestedBalanceCanBeIterated() {
		// Arrange:
		final VestedBalance initialBalance = new VestedBalance(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		final VestedBalance vestedBalance = initialBalance.next();

		// Assert:
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1)));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(16)));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(984)));
	}

	@Test
	public void vestedBalanceAfter50Days() {
		// Arrange:
		final VestedBalance initialBalance = new VestedBalance(BlockHeight.ONE, Amount.fromMicroNem(1000));

		// Act
		VestedBalance vestedBalance = initialBalance;
		for (int i=0; i<50; ++i) {
			vestedBalance = vestedBalance.next();
		}

		// Assert:
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY*50 + 1)));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(545)));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(455)));
	}

	@Test
	public void nemsAreNotLostDuringIterationOfTinyAmounts() {
		// Arrange:
		final VestedBalance initialBalance = new VestedBalance(BlockHeight.ONE, Amount.fromMicroNem(75));

		// Act
		final VestedBalance vestedBalance = initialBalance.next();

		// Assert:
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(new BlockHeight(BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + 1)));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(1)));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(74)));
	}

	@Test
	public void nemsAreProperlyRestored() {
		for (long i = 1; i < 1000; ++i) {
			singleTest(i);
		}
	}

	public void singleTest(long amount) {
		// Arrange:
		final VestedBalance initialBalance = new VestedBalance(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act
		final VestedBalance dummyBalance = initialBalance.next();
		final VestedBalance vestedBalance = dummyBalance.previous();

		// Assert:
		Assert.assertThat(vestedBalance.getBlockHeight(), IsEqual.equalTo(BlockHeight.ONE));
		Assert.assertThat(vestedBalance.getUnvestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(amount)));
		Assert.assertThat(vestedBalance.getVestedBalance(), IsEqual.equalTo(Amount.fromMicroNem(0)));
	}
	//endregion

	//region send/undoSend
	@Test
	public void sendingKeepsRatio() {
		// Arrange:
		final VestedBalance vestedBalance = prepareVestedForSending(10_000);

		// Act:
		double ratio1 = vestedBalance.getVestedBalance().getNumMicroNem() / (0d + vestedBalance.getVestedBalance().getNumMicroNem() + vestedBalance.getUnvestedBalance().getNumMicroNem());
		vestedBalance.send(Amount.fromMicroNem(2_000));
		double ratio2 = vestedBalance.getVestedBalance().getNumMicroNem() / (0d + vestedBalance.getVestedBalance().getNumMicroNem() + vestedBalance.getUnvestedBalance().getNumMicroNem());

		// Assert:
		Assert.assertTrue((ratio1 - ratio2) < Double.MIN_NORMAL);
	}

	@Test
	public void undoSendRestoresValues() {
		// Arrange:
		final VestedBalance vestedBalance = prepareVestedForSending(100_000);

		// Act:
		final Amount vested1 = vestedBalance.getVestedBalance();
		final Amount unvested1 = vestedBalance.getUnvestedBalance();
		vestedBalance.send(Amount.fromMicroNem(20_000));
		vestedBalance.undoSend(Amount.fromMicroNem(20_000));
		final Amount vested2 = vestedBalance.getVestedBalance();
		final Amount unvested2 = vestedBalance.getUnvestedBalance();

		// Assert:
		Assert.assertThat(vested2, IsEqual.equalTo(vested1));
		Assert.assertThat(unvested2, IsEqual.equalTo(unvested1));
	}

	@Test
	public void undoSendDoesNotFail() {
		// Arrange:
		final VestedBalance vestedBalance = prepareVestedForSending(100_000);

		// Act:
		final Amount vested1 = vestedBalance.getVestedBalance();
		final Amount unvested1 = vestedBalance.getUnvestedBalance();
		vestedBalance.send(Amount.fromMicroNem(100_000));
		vestedBalance.undoSend(Amount.fromMicroNem(100_000));
		final Amount vested2 = vestedBalance.getVestedBalance();
		final Amount unvested2 = vestedBalance.getUnvestedBalance();

		// Assert:
		Assert.assertThat(vested2, IsEqual.equalTo(vested1));
		Assert.assertThat(unvested2, IsEqual.equalTo(unvested1));
	}

	@Test
	public void undoSendRestoresValuesMultipleSends() {
		// Arrange:
		for (int i = 1; i <= 10; ++i) {
			final VestedBalance vestedBalance = prepareVestedForSending(1_000_000);

			// Act:
			final Amount vested1 = vestedBalance.getVestedBalance();
			final Amount unvested1 = vestedBalance.getUnvestedBalance();
			for (int j = 0; j < i; ++j) {
				vestedBalance.send(Amount.fromMicroNem(20_000 + j));
			}
			for (int j = 0; j < i; ++j) {
				vestedBalance.undoSend(Amount.fromMicroNem(20_000 + (i - j - 1)));
			}
			final Amount vested2 = vestedBalance.getVestedBalance();
			final Amount unvested2 = vestedBalance.getUnvestedBalance();

			// Assert:
			Assert.assertThat(vested2, IsEqual.equalTo(vested1));
			Assert.assertThat(unvested2, IsEqual.equalTo(unvested1));
		}
	}

	private VestedBalance prepareVestedForSending(long amount) {
		// Arrange:
		final VestedBalance initialBalance = new VestedBalance(BlockHeight.ONE, Amount.fromMicroNem(amount));

		// Act:
		VestedBalance vestedBalance = initialBalance;
		for (int i=0; i<30; ++i) {
			vestedBalance = vestedBalance.next();
		}
		return vestedBalance;
	}
	//endregion


	@Test
	public void vestedBalanceCanBeCompared() {
		// Arrange:
		final VestedBalance vestedBalance1 = new VestedBalance(new BlockHeight(10), Amount.fromMicroNem(1000));
		final VestedBalance vestedBalance2 = new VestedBalance(new BlockHeight(20), Amount.fromMicroNem(1000));

		// Assert:
		Assert.assertThat(vestedBalance1.compareTo(vestedBalance1), IsEqual.equalTo(0));
		Assert.assertThat(vestedBalance1.compareTo(vestedBalance2), IsEqual.equalTo(-1));
		Assert.assertThat(vestedBalance2.compareTo(vestedBalance1), IsEqual.equalTo(1));
	}


}
