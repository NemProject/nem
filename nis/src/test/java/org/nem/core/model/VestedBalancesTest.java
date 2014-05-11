package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class VestedBalancesTest {
	//region addReceive
	@Test
	public void canAddToEmptyBalances() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();
		final VestedBalance referenceBalance = new VestedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1441, referenceBalance.next().getUnvestedBalance());
	}

	@Test
	public void addWithinSameBucketProducesCorrectResult() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();
		final VestedBalance referenceBalance = new VestedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(100));
		vestedBalances.addReceive(new BlockHeight(1440), Amount.fromNem(23));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1441, referenceBalance.next().getUnvestedBalance());
	}

	@Test
	public void addSpanningAcrossGroupsProducesCorrectResults() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();
		VestedBalance referenceBalance = new VestedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(345));

		referenceBalance = referenceBalance.next();
		referenceBalance = referenceBalance.next();
		referenceBalance.receive(Amount.fromNem(345));

		// Assert:
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
		assertUnvested(vestedBalances, 2881, referenceBalance.getUnvestedBalance());
		assertUnvested(vestedBalances, 2881 + 1440, Amount.fromMicroNem(456933369)); // referenceBalance.next().getUnvestedBalance()
	}
	//endregion

	//region receiveUndo
	@Test
	public void undoRestoresProperBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();
		VestedBalance referenceBalance = new VestedBalance(new BlockHeight(1440), Amount.fromNem(123));

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addReceive(new BlockHeight(2881), Amount.fromNem(345));
		final Amount afterNext = vestedBalances.getUnvested(new BlockHeight(2881 + 1440));
		vestedBalances.undoReceive(new BlockHeight(2881), Amount.fromNem(345));

		referenceBalance = referenceBalance.next();
		referenceBalance = referenceBalance.next();
		referenceBalance.receive(Amount.fromNem(345));
		referenceBalance.undoReceive(Amount.fromNem(345));
		referenceBalance = referenceBalance.next();

		// Assert:
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
		Assert.assertThat(afterNext, IsEqual.equalTo(Amount.fromMicroNem(456933369)));
		assertUnvested(vestedBalances, 2881 + 1440, referenceBalance.getUnvestedBalance());
	}
	//endregion

	//region addSend
	@Test(expected = IllegalArgumentException.class)
	public void cannotSendFromEmptyBalances() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));
	}

	@Test
	public void canSendBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(100));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(100));
	}

	@Test
	public void canSendWholeBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(0));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(0));
	}

	@Test
	public void canUndoSendBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));
		vestedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
	}


	@Test
	public void canUndoSendWholeBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
	}

	@Test
	public void canUndoAfterTimeSendBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(23));
		vestedBalances.getUnvested(new BlockHeight(1441));
		vestedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(23));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
	}

	@Test
	public void canUndoSendBalanceAfterTime() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances();

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(123));
		vestedBalances.addSend(new BlockHeight(1441), Amount.fromNem(23));
		vestedBalances.undoSend(new BlockHeight(1441), Amount.fromNem(23));

		// Assert:
		assertUnvested(vestedBalances, 1, Amount.fromNem(123));
		assertUnvested(vestedBalances, 1440, Amount.fromNem(123));
	}
	//endregion

	private void assertUnvested(final VestedBalances vestedBalances, long height, final Amount amount) {
		Assert.assertThat(vestedBalances.getUnvested(new BlockHeight(height)), IsEqual.equalTo(amount));
	}
}
