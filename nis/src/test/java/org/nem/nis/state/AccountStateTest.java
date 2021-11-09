package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.RemoteLinkFactory;

public class AccountStateTest {

	// region creation

	@Test
	public void accountStateCanBeCreated() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState state = new AccountState(address);

		// Assert:
		MatcherAssert.assertThat(state.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(state.getWeightedBalances(), IsNull.notNullValue());
		MatcherAssert.assertThat(state.getHistoricalImportances(), IsNull.notNullValue());
		MatcherAssert.assertThat(state.getImportanceInfo().isSet(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(state.getRemoteLinks(), IsNull.notNullValue());
		MatcherAssert.assertThat(state.getAccountInfo(), IsNull.notNullValue());
		MatcherAssert.assertThat(state.getMultisigLinks(), IsNull.notNullValue());
		MatcherAssert.assertThat(state.getHeight(), IsNull.nullValue());
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState state = new AccountState(address);

		// Act:
		final AccountState copy = state.copy();

		// Assert:
		MatcherAssert.assertThat(copy.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountImportance() {
		// Arrange:
		final BlockHeight height = new BlockHeight(123);
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final AccountImportance importance = state.getImportanceInfo();
		importance.setImportance(height, 0.03125);
		importance.addOutlink(new AccountLink(height, Amount.fromNem(12), Utils.generateRandomAddress()));

		// Act:
		final AccountState copy = state.copy();
		final ReadOnlyAccountImportance copyImportance = copy.getImportanceInfo();
		copy.getImportanceInfo().setImportance(new BlockHeight(234), 0.0234375);

		// Assert:
		MatcherAssert.assertThat(copyImportance, IsNot.not(IsSame.sameInstance(importance)));
		MatcherAssert.assertThat(importance.getImportance(height), IsEqual.equalTo(0.03125));
		MatcherAssert.assertThat(copyImportance.getImportance(new BlockHeight(234)), IsEqual.equalTo(0.0234375));
		MatcherAssert.assertThat(copyImportance.getOutlinksSize(height), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copyImportance.getOutlinksIterator(height, height).next(),
				IsEqual.equalTo(importance.getOutlinksIterator(height, height).next()));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfWeightedBalances() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final WeightedBalances balances = state.getWeightedBalances();
		balances.addReceive(new BlockHeight(17), Amount.fromNem(1234));

		// Act:
		final AccountState copy = state.copy();
		final ReadOnlyWeightedBalances copyBalances = copy.getWeightedBalances();

		// Assert:
		MatcherAssert.assertThat(copyBalances, IsNot.not(IsSame.sameInstance(balances)));
		MatcherAssert.assertThat(copyBalances.getUnvested(new BlockHeight(17)), IsEqual.equalTo(Amount.fromNem(1234)));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfHistoricalImportances() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final HistoricalImportances importances = state.getHistoricalImportances();
		importances.addHistoricalImportance(new AccountImportance(new BlockHeight(17), 0.3, 0.6));

		// Act:
		final AccountState copy = state.copy();
		final ReadOnlyHistoricalImportances copyImportances = copy.getHistoricalImportances();

		// Assert:
		MatcherAssert.assertThat(copyImportances, IsNot.not(IsSame.sameInstance(importances)));
		MatcherAssert.assertThat(copyImportances.getHistoricalImportance(new BlockHeight(17)), IsEqual.equalTo(0.3));
		MatcherAssert.assertThat(copyImportances.getHistoricalPageRank(new BlockHeight(17)), IsEqual.equalTo(0.6));
	}

	@Test
	public void copyCopiesHeightRemoteLinks() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final RemoteLinks links = state.getRemoteLinks();
		final RemoteLink link1 = RemoteLinkFactory.activateRemoteHarvester(Address.fromEncoded("a"), new BlockHeight(7));
		final RemoteLink link2 = RemoteLinkFactory.activateRemoteHarvester(Address.fromEncoded("b"), new BlockHeight(7));
		final RemoteLink link3 = RemoteLinkFactory.activateRemoteHarvester(Address.fromEncoded("c"), new BlockHeight(7));
		links.addLink(link1);
		links.addLink(link2);
		links.addLink(link3);

		// Act:
		final AccountState copy = state.copy();

		// Act:
		final RemoteLinks copyLinks = copy.getRemoteLinks();
		copyLinks.removeLink(link3);
		copyLinks.removeLink(link2);

		// Assert:
		MatcherAssert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(copyLinks.isEmpty(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(copyLinks.getCurrent(), IsNull.nullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountInfo() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		state.getAccountInfo().incrementBalance(Amount.fromNem(1234));

		// Act:
		final AccountState copy = state.copy();
		copy.getAccountInfo().decrementBalance(Amount.fromNem(234));

		// Assert:
		MatcherAssert.assertThat(state.getAccountInfo().getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		MatcherAssert.assertThat(copy.getAccountInfo().getBalance(), IsEqual.equalTo(Amount.fromNem(1000)));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfMultisigLinks() {
		// Arrange:
		final Address multisig1 = Utils.generateRandomAddress();
		final Address multisig2 = Utils.generateRandomAddress();
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final MultisigLinks stateLinks = state.getMultisigLinks();
		stateLinks.addCosignatoryOf(multisig1);

		// Act:
		final AccountState copy = state.copy();
		final MultisigLinks copyLinks = copy.getMultisigLinks();
		copyLinks.addCosignatoryOf(multisig2);

		// Assert:
		MatcherAssert.assertThat(stateLinks.isCosignatoryOf(multisig1), IsEqual.equalTo(true));
		MatcherAssert.assertThat(stateLinks.isCosignatoryOf(multisig2), IsEqual.equalTo(false));

		MatcherAssert.assertThat(copyLinks.isCosignatoryOf(multisig1), IsEqual.equalTo(true));
		MatcherAssert.assertThat(copyLinks.isCosignatoryOf(multisig2), IsEqual.equalTo(true));
	}

	@Test
	public void copyCopiesHeight() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		state.setHeight(new BlockHeight(17));

		// Act:
		final AccountState copy = state.copy();

		// Assert:
		MatcherAssert.assertThat(copy.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	// endregion

	// region height

	@Test
	public void accountHeightCanBeSetIfNull() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());

		// Act:
		state.setHeight(new BlockHeight(17));

		// Assert:
		MatcherAssert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	@Test
	public void accountHeightCannotBeUpdatedIfNonNull() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());

		// Act:
		state.setHeight(new BlockHeight(17));
		state.setHeight(new BlockHeight(32));

		// Assert:
		MatcherAssert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	// endregion
}
