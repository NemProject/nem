package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class WeightedBalancesTest {

	//region addReceive

	@Test
	public void canAddToEmptyBalances() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();
		final WeightedBalance referenceBalance = new WeightedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1441, referenceBalance.next().getUnvestedBalance());
	}

	@Test
	public void addWithinSameBucketProducesCorrectResult() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();
		final WeightedBalance referenceBalance = new WeightedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(100));
		weightedBalances.addReceive(new BlockHeight(1440), Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1441, referenceBalance.next().getUnvestedBalance());
	}

	@Test
	public void addSpanningAcrossGroupsProducesCorrectResults() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();
		WeightedBalance referenceBalance = new WeightedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(345));

		referenceBalance = referenceBalance.next();
		referenceBalance = referenceBalance.next();
		referenceBalance.receive(Amount.fromNem(345));

		// Assert:
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		assertUnvested(weightedBalances, 2881, referenceBalance.getUnvestedBalance());
		assertUnvested(weightedBalances, 2881 + 1440, referenceBalance.next().getUnvestedBalance());
		assertUnvested(weightedBalances, 2881 + 1440, Amount.fromMicroNem(400_167_000L));
	}

	//endregion

	//region receiveUndo

	@Test
	public void undoRestoresProperBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();
		WeightedBalance referenceBalance = new WeightedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(345));
		final Amount afterNext = weightedBalances.getUnvested(new BlockHeight(2881 + 1440));
		weightedBalances.undoReceive(new BlockHeight(2881), Amount.fromNem(345));

		referenceBalance = referenceBalance.next();
		referenceBalance = referenceBalance.next();
		referenceBalance.receive(Amount.fromNem(345));
		referenceBalance.undoReceive(Amount.fromNem(345));
		referenceBalance = referenceBalance.next();

		// Assert:
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
		// use previous test as a reference to obtain proper value here
		Assert.assertThat(afterNext, IsEqual.equalTo(Amount.fromMicroNem(400_167_000L)));
		assertUnvested(weightedBalances, 2881 + 1440, referenceBalance.getUnvestedBalance());
	}

	//endregion

	//region addSend

	@Test(expected = IllegalArgumentException.class)
	public void cannotSendFromEmptyBalances() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));
	}

	@Test
	public void canSendBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(100));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(100));
	}

	@Test
	public void canSendWholeBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.ZERO);
		assertUnvested(weightedBalances, 1440, Amount.ZERO);
	}

	@Test
	public void canUndoSendBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
	}

	@Test
	public void canUndoSendWholeBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
	}

	@Test
	public void canUndoSendPartiallyMaturedWholeBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(new BlockHeight(1441), Amount.fromNem(123));
		weightedBalances.undoSend(new BlockHeight(1441), Amount.fromNem(123));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1441, Amount.fromMicroNem(110700000));
	}

	@Test
	public void canUndoSendWholeBalanceCumulative() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(10_000));
		weightedBalances.addReceive(new BlockHeight(1440), Amount.fromNem(2_000));
		weightedBalances.addReceive(new BlockHeight(1441), Amount.fromNem(300));
		weightedBalances.addReceive(new BlockHeight(2880), Amount.fromNem(40));
		weightedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(5));
		weightedBalances.addSend(new BlockHeight(2882), Amount.fromNem(12_345));
		weightedBalances.undoSend(new BlockHeight(2882), Amount.fromNem(12_345));

		// Assert:
		assertSum(weightedBalances, 2880, Amount.fromNem(12_340));
		assertSum(weightedBalances, 2881, Amount.fromNem(12_345));
		assertSum(weightedBalances, 2882, Amount.fromNem(12_345));
	}

	@Test
	public void canUndoAfterTimeSendBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new WeightedBalances();

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));
		assertUnvested(weightedBalances, 1, Amount.fromNem(100));

		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(weightedBalances, 1, Amount.fromNem(123));
		assertUnvested(weightedBalances, 1440, Amount.fromNem(123));
	}

	//endregion

	private void assertUnvested(final WeightedBalances weightedBalances, long height, final Amount amount) {
		Assert.assertThat(weightedBalances.getUnvested(new BlockHeight(height)), IsEqual.equalTo(amount));
	}

	private void assertSum(final WeightedBalances weightedBalances, long height, final Amount amount) {
		final BlockHeight blockHeight = new BlockHeight(height);
		final Amount actualSum = weightedBalances.getUnvested(blockHeight).add(weightedBalances.getVested(blockHeight));
		Assert.assertThat(actualSum, IsEqual.equalTo(amount));
	}
}
