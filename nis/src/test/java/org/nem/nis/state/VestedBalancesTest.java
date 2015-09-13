package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;

public class VestedBalancesTest {

	// region ctor

	@Test
	public void canCreateVestedBalancesWithoutAmountParameter() {
		// Act:
		final VestedBalances vestedBalances = new VestedBalances();

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void canCreateVestedBalancesWithAmountParameter() {
		// Act:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	// endregion

	// region independence of block height

	@Test
	public void balanceDoesNotChangeWithBlockHeight() {
		// Act:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getUnvested(new BlockHeight(123)), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getUnvested(new BlockHeight(12345)), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getUnvested(new BlockHeight(1234567)), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.MAX), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(vestedBalances.getVested(new BlockHeight(123)), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(vestedBalances.getVested(new BlockHeight(12345)), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(vestedBalances.getVested(new BlockHeight(1234567)), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.MAX), IsEqual.equalTo(Amount.fromNem(123)));

	}

	// endregion

	// region ReadOnlyVestingBalance

	@Test
	public void UnvestedBalanceIsAlwaysZero() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act + Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		vestedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		vestedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		vestedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void VestedBalanceIsAlwaysFullBalance() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act + Assert:
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
		vestedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345)));
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567)));
		vestedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456)));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456 - 111)));
		vestedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456 - 111 + 222)));
	}

	@Test
	public void sizeIsAlwaysOne() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act + Assert:
		Assert.assertThat(vestedBalances.size(), IsEqual.equalTo(1));
		vestedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		Assert.assertThat(vestedBalances.size(), IsEqual.equalTo(1));
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		Assert.assertThat(vestedBalances.size(), IsEqual.equalTo(1));
		vestedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		Assert.assertThat(vestedBalances.size(), IsEqual.equalTo(1));
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		Assert.assertThat(vestedBalances.size(), IsEqual.equalTo(1));
		vestedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		Assert.assertThat(vestedBalances.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region VestingBalances

	@Test
	public void copyReturnsVestingBalanceWithSameBalance() {
		// Arrange:
		final VestedBalances original = new VestedBalances(Amount.fromNem(123));

		// Act:
		final VestingBalances copy = original.copy();

		// Assert:
		Assert.assertThat(copy.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(copy.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfVestingBalances() {
		// Arrange:
		final VestedBalances original = new VestedBalances(Amount.fromNem(123));
		final VestingBalances copy = original.copy();

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
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act:
		vestedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 234)));
	}

	@Test
	public void addReceiveIncrementsBalanceByGivenAmount() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 234)));
	}

	@Test
	public void undoReceiveDecrementsBalanceByGivenAmount() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(234));

		// Act:
		vestedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(234 - 123)));
	}

	@Test
	public void addSendDecrementsBalanceByGivenAmount() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(234));

		// Act:
		vestedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(234 - 123)));
	}

	@Test
	public void undoSendIncrementsBalanceByGivenAmount() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act:
		vestedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 234)));
	}

	@Test
	public void convertToFullyVestedDoesNothing() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act:
		vestedBalances.convertToFullyVested();

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void undoChainDoesNothing() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act:
		vestedBalances.undoChain(BlockHeight.ONE);

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	@Test
	public void pruneDoesNothing() {
		// Arrange:
		final VestedBalances vestedBalances = new VestedBalances(Amount.fromNem(123));

		// Act:
		vestedBalances.prune(BlockHeight.ONE);

		// Assert:
		Assert.assertThat(vestedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(vestedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
	}

	// endregion
}
