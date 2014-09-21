package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.secret.*;

public class PoiAccountStateTest {

	//region creation

	@Test
	public void poiAccountStateCanBeCreated() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiAccountState state = new PoiAccountState(address);

		// Assert:
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(state.getWeightedBalances(), IsNull.notNullValue());
		Assert.assertThat(state.getImportanceInfo().isSet(), IsEqual.equalTo(false));
		Assert.assertThat(state.getRemoteLinks(), IsNull.notNullValue());
		Assert.assertThat(state.getHeight(), IsNull.nullValue());
	}

	//endregion

	//region copy

	@Test
	public void copyCopiesAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiAccountState state = new PoiAccountState(address);

		// Act:
		final PoiAccountState copy = state.copy();

		// Assert:
		Assert.assertThat(copy.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountImportance() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
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
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		final WeightedBalances balances = state.getWeightedBalances();
		balances.addReceive(new BlockHeight(17), Amount.fromNem(1234));

		// Act:
		final PoiAccountState copy = state.copy();
		final WeightedBalances copyBalances = copy.getWeightedBalances();

		// Assert:
		Assert.assertThat(copyBalances, IsNot.not(IsSame.sameInstance(balances)));
		Assert.assertThat(copyBalances.getUnvested(new BlockHeight(17)), IsEqual.equalTo(Amount.fromNem(1234)));
	}

	@Test
	public void copyCopiesHeightRemoteLinks() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		final RemoteLinks links = state.getRemoteLinks();
		final RemoteLink link1 = new RemoteLink(Address.fromEncoded("a"), new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);;
		final RemoteLink link2 = new RemoteLink(Address.fromEncoded("b"), new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);;
		final RemoteLink link3 = new RemoteLink(Address.fromEncoded("c"), new BlockHeight(7), 1, RemoteLink.Owner.RemoteHarvester);;
		links.addLink(link1);
		links.addLink(link2);
		links.addLink(link3);

		// Act:
		final PoiAccountState copy = state.copy();

		// Act:
		final RemoteLinks copyLinks = copy.getRemoteLinks();
		copyLinks.removeLink(link3);
		copyLinks.removeLink(link2);

		// Assert:
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(copyLinks.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(copyLinks.getCurrent(), IsNull.nullValue());
	}

	@Test
	public void copyCopiesHeight() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());
		state.setHeight(new BlockHeight(17));

		// Act:
		final PoiAccountState copy = state.copy();

		// Assert:
		Assert.assertThat(copy.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	//endregion

	//region height

	@Test
	public void accountHeightCanBeSetIfNull() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());

		// Act:
		state.setHeight(new BlockHeight(17));

		// Assert:
		Assert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	@Test
	public void accountHeightCannotBeUpdatedIfNonNull() {
		// Arrange:
		final PoiAccountState state = new PoiAccountState(Utils.generateRandomAddress());

		// Act:
		state.setHeight(new BlockHeight(17));
		state.setHeight(new BlockHeight(32));

		// Assert:
		Assert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	//endregion
}