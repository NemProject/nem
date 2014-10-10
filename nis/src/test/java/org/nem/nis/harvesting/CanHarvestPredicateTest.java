package org.nem.nis.harvesting;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.PoiAccountState;

public class CanHarvestPredicateTest {

	@Test
	public void cannotHarvestWhenVestedBalanceIsLessThanMinimumBalance() {
		// Arrange:
		final Amount minBalanceMinusOne = Amount.fromMicroNem(22222221);

		// Assert:
		Assert.assertThat(canHarvest(Amount.ZERO), IsEqual.equalTo(false));
		Assert.assertThat(canHarvest(minBalanceMinusOne), IsEqual.equalTo(false));
	}

	@Test
	public void canHarvestWhenVestedBalanceIsAtLeastMinimumBalance() {
		// Arrange:
		final Amount minBalance = Amount.fromNem(22222222);
		final Amount twiceMinBalance = Amount.fromNem(44444444);

		// Assert:
		Assert.assertThat(canHarvest(minBalance), IsEqual.equalTo(true));
		Assert.assertThat(canHarvest(twiceMinBalance), IsEqual.equalTo(true));
	}

	private static boolean canHarvest(final Amount vestedBalance) {
		// Arrange:
		final BlockHeight height = new BlockHeight(33);
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addFullyVested(height, vestedBalance);

		// Act:
		return new CanHarvestPredicate(Amount.fromMicroNem(22222222)).canHarvest(state, height);
	}
}