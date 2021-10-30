package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;

public class NemStateGlobalsTest {

	@After
	public void resetGlobals() {
		NemStateGlobals.setWeightedBalancesSupplier(null);
	}

	// region weighted balances supplier

	@Test
	public void canCreateWeightedBalancesUsingDefaultSupplier() {
		// Assert:
		MatcherAssert.assertThat(NemStateGlobals.createWeightedBalances(), IsInstanceOf.instanceOf(TimeBasedVestingWeightedBalances.class));
	}

	@Test
	public void canCreateWeightedBalancesUsingCustomSupplier() {
		// Arrange:
		final WeightedBalances balances = Mockito.mock(WeightedBalances.class);

		// Act:
		NemStateGlobals.setWeightedBalancesSupplier(() -> balances);

		// Assert:
		MatcherAssert.assertThat(NemStateGlobals.createWeightedBalances(), IsEqual.equalTo(balances));
	}

	// endregion
}
