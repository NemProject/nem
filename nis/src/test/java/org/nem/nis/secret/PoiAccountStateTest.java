package org.nem.nis.secret;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class PoiAccountStateTest {

	//region creation

	@Test
	public void constructorInitializesWeightedBalances() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState();

		// Assert:
		Assert.assertThat(state.getWeightedBalances(), IsNull.notNullValue());
	}

	@Test
	public void constructorInitializesImportanceInfoAsUnset() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState();

		// Assert:
		Assert.assertThat(state.getImportanceInfo().isSet(), IsEqual.equalTo(false));
	}

	//endregion

	//region copy

	@Test
	public void copyCreatesUnlinkedCopyOfAccountImportance() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState();
		final AccountImportance importance = state.getImportanceInfo();
		importance.setImportance(BlockHeight.ONE, 0.03125);
		importance.addOutlink(new AccountLink(BlockHeight.ONE, Amount.fromNem(12), Utils.generateRandomAddress()));

		// Act:
		final PoiAccountState copy = state.copy();
		final AccountImportance copyImportance = copy.getImportanceInfo();
		copy.getImportanceInfo().setImportance(new BlockHeight(2), 0.0234375);

		// Assert:
		Assert.assertThat(copyImportance, IsNot.not(IsSame.sameInstance(importance)));
		Assert.assertThat(importance.getImportance(BlockHeight.ONE), IsEqual.equalTo(0.03125));
		Assert.assertThat(copyImportance.getImportance(new BlockHeight(2)), IsEqual.equalTo(0.0234375));
		Assert.assertThat(copyImportance.getOutlinksSize(BlockHeight.ONE), IsEqual.equalTo(1));
		Assert.assertThat(
				copyImportance.getOutlinksIterator(BlockHeight.ONE).next(),
				IsEqual.equalTo(importance.getOutlinksIterator(BlockHeight.ONE).next()));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfWeightedBalances() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState();
		final WeightedBalances balances = state.getWeightedBalances();
		balances.addReceive(new BlockHeight(17), Amount.fromNem(1234));

		// Act:
		final PoiAccountState copy = state.copy();
		final WeightedBalances copyBalances = copy.getWeightedBalances();

		// Assert:
		Assert.assertThat(copyBalances, IsNot.not(IsSame.sameInstance(balances)));
		Assert.assertThat(copyBalances.getUnvested(new BlockHeight(17)), IsEqual.equalTo(Amount.fromNem(1234)));
	}

	//endregion
}