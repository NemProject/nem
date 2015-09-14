package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;

public class AlwaysVestedBalancesTest {

	// region ctor

	@Test
	public void canCreateVestedBalancesWithoutAmountParameter() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances();

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void canCreateVestedBalancesWithAmountParameter() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	// endregion

	// region independence of block height

	@Test
	public void balanceDoesNotChangeWithBlockHeight() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getUnvested(new BlockHeight(123)), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getUnvested(new BlockHeight(12345)), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getUnvested(new BlockHeight(1234567)), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.MAX), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(weightedBalances.getVested(new BlockHeight(123)), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(weightedBalances.getVested(new BlockHeight(12345)), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(weightedBalances.getVested(new BlockHeight(1234567)), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.MAX), IsEqual.equalTo(Amount.fromNem(123)));

	}

	// endregion

	// region ReadOnlyVestingBalance

	@Test
	public void UnvestedBalanceIsAlwaysZero() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act + Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void VestedBalanceIsAlwaysFullBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act + Assert:
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345)));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567)));
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456)));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456 - 111)));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456 - 111 + 222)));
	}

	@Test
	public void sizeIsAlwaysOne() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act + Assert:
		Assert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		Assert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		Assert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		Assert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		Assert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		Assert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region VestingBalances

	@Test
	public void copyReturnsVestingBalanceWithSameBalance() {
		// Arrange:
		final WeightedBalances original = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		final WeightedBalances copy = original.copy();

		// Assert:
		Assert.assertThat(copy.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(copy.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfVestingBalances() {
		// Arrange:
		final WeightedBalances original = new AlwaysVestedBalances(Amount.fromNem(123));
		final WeightedBalances copy = original.copy();

		// Act:
		original.addSend(BlockHeight.ONE, Amount.fromNem(123));
		original.addReceive(BlockHeight.ONE, Amount.fromNem(321));

		// Assert:
		Assert.assertThat(copy.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(copy.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void addFullyVestedIncrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 234)));
	}

	@Test
	public void addReceiveIncrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 234)));
	}

	@Test
	public void undoReceiveDecrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(234));

		// Act:
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(234 - 123)));
	}

	@Test
	public void addSendDecrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(234));

		// Act:
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(234 - 123)));
	}

	@Test
	public void undoSendIncrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 234)));
	}

	@Test
	public void convertToFullyVestedDoesNothing() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.convertToFullyVested();

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void undoChainDoesNothing() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.undoChain(BlockHeight.ONE);

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void pruneDoesNothing() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.prune(BlockHeight.ONE);

		// Assert:
		Assert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	// endregion
}
