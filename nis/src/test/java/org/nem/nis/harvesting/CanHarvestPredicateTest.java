package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.state.AccountState;

public class CanHarvestPredicateTest {

	@Test
	public void cannotHarvestWhenVestedBalanceIsLessThanMinimumBalance() {
		// Arrange:
		final Amount minBalanceMinusOne = Amount.fromMicroNem(22222221);

		// Assert:
		MatcherAssert.assertThat(canHarvest(Amount.ZERO), IsEqual.equalTo(false));
		MatcherAssert.assertThat(canHarvest(minBalanceMinusOne), IsEqual.equalTo(false));
	}

	@Test
	public void canHarvestWhenVestedBalanceIsAtLeastMinimumBalance() {
		// Arrange:
		final Amount minBalance = Amount.fromNem(22222222);
		final Amount twiceMinBalance = Amount.fromNem(44444444);

		// Assert:
		MatcherAssert.assertThat(canHarvest(minBalance), IsEqual.equalTo(true));
		MatcherAssert.assertThat(canHarvest(twiceMinBalance), IsEqual.equalTo(true));
	}

	@Test
	public void canHarvestPredicateSupportsMinBalanceDependentOnBlockHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(2);
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addFullyVested(height, Amount.fromNem(22222222));

		final CanHarvestPredicate predicate = new CanHarvestPredicate(h -> {
			return Amount.fromNem(11111111 * (h.getRaw() - 1));
		});

		// Assert:
		MatcherAssert.assertThat(predicate.canHarvest(state, new BlockHeight(2)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(predicate.canHarvest(state, new BlockHeight(3)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(predicate.canHarvest(state, new BlockHeight(4)), IsEqual.equalTo(false));
	}

	private static boolean canHarvest(final Amount vestedBalance) {
		// Arrange:
		final BlockHeight height = new BlockHeight(33);
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addFullyVested(height, vestedBalance);

		// Act:
		return new CanHarvestPredicate(Amount.fromMicroNem(22222222)).canHarvest(state, height);
	}
}
