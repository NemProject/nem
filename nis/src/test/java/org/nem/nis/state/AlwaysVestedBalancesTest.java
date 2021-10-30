package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;

import java.util.*;

public class AlwaysVestedBalancesTest {

	// region ctor

	@Test
	public void canCreateBalancesWithoutAmountParameter() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances();

		// Assert:
		assertBalance(weightedBalances, Amount.ZERO);
	}

	@Test
	public void canCreateBalancesWithAmountParameter() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123));
	}

	// endregion

	// region independence of block height

	@Test
	public void unvestedBalanceIsIndependentOfBlockHeight() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Assert:

		for (final BlockHeight height : getBlockHeights()) {
			MatcherAssert.assertThat(weightedBalances.getUnvested(height), IsEqual.equalTo(Amount.ZERO));
		}
	}

	@Test
	public void vestedBalanceIsIndependentOfBlockHeight() {
		// Act:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Assert:
		for (final BlockHeight height : getBlockHeights()) {
			MatcherAssert.assertThat(weightedBalances.getVested(height), IsEqual.equalTo(Amount.fromNem(123)));
		}
	}

	private static Collection<BlockHeight> getBlockHeights() {
		return Arrays.asList(BlockHeight.ONE, new BlockHeight(123), new BlockHeight(12345), new BlockHeight(511000), BlockHeight.MAX);
	}

	// endregion

	// region ReadOnlyWeightedBalances

	@Test
	public void unvestedBalanceIsAlwaysZero() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act + Assert:
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
	}

	@Test
	public void vestedBalanceIsAlwaysFullBalance() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act + Assert:
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123)));
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345)));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567)));
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456)));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456 - 111)));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE),
				IsEqual.equalTo(Amount.fromNem(123 + 345 + 567 - 456 - 111 + 222)));
	}

	@Test
	public void sizeIsAlwaysOne() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act + Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(345));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(567));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(456));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(111));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(222));
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region WeightedBalances

	@Test
	public void copyReturnsWeightedBalancesWithSameBalance() {
		// Arrange:
		final WeightedBalances original = new AlwaysVestedBalances(Amount.fromNem(123));
		original.addReceive(BlockHeight.ONE, Amount.fromNem(34));

		// Act:
		final WeightedBalances copy = original.copy();

		// Assert:
		assertBalance(copy, Amount.fromNem(157));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfWeightedBalances() {
		// Arrange:
		final WeightedBalances original = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		final WeightedBalances copy = original.copy();
		original.addSend(BlockHeight.ONE, Amount.fromNem(123));
		original.addReceive(BlockHeight.ONE, Amount.fromNem(321));

		// Assert:
		assertBalance(original, Amount.fromNem(321));
		assertBalance(copy, Amount.fromNem(123));
	}

	@Test
	public void addFullyVestedIncrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.addFullyVested(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123 + 234));
	}

	@Test
	public void addReceiveIncrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.addReceive(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123 + 234));
	}

	@Test
	public void undoReceiveDecrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(234));

		// Act:
		weightedBalances.undoReceive(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(234 - 123));
	}

	@Test
	public void addSendDecrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(234));

		// Act:
		weightedBalances.addSend(BlockHeight.ONE, Amount.fromNem(123));

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(234 - 123));
	}

	@Test
	public void undoSendIncrementsBalanceByGivenAmount() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.undoSend(BlockHeight.ONE, Amount.fromNem(234));

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123 + 234));
	}

	@Test
	public void convertToFullyVestedDoesNothing() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.convertToFullyVested();

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123));
	}

	@Test
	public void undoChainDoesNothing() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.undoChain(BlockHeight.ONE);

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123));
	}

	@Test
	public void pruneDoesNothing() {
		// Arrange:
		final WeightedBalances weightedBalances = new AlwaysVestedBalances(Amount.fromNem(123));

		// Act:
		weightedBalances.prune(BlockHeight.ONE);

		// Assert:
		assertBalance(weightedBalances, Amount.fromNem(123));
	}

	// endregion

	private static void assertBalance(final ReadOnlyWeightedBalances weightedBalances, final Amount expectedVestedAmount) {
		// Assert:
		MatcherAssert.assertThat(weightedBalances.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(weightedBalances.getUnvested(BlockHeight.ONE), IsEqual.equalTo(Amount.ZERO));
		MatcherAssert.assertThat(weightedBalances.getVested(BlockHeight.ONE), IsEqual.equalTo(expectedVestedAmount));
	}
}
