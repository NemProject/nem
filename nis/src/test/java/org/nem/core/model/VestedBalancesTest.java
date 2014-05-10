package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class VestedBalancesTest {
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

	private void assertUnvested(final VestedBalances vestedBalances, long height, final Amount amount) {
		Assert.assertThat(vestedBalances.getUnvested(new BlockHeight(height)), IsEqual.equalTo(amount));
	}
}
