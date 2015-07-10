package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.test.RemoteLinkFactory;

public class AccountStateTest {

	//region creation

	@Test
	public void accountStateCanBeCreated() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState state = new AccountState(address);

		// Assert:
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(state.getWeightedBalances(), IsNull.notNullValue());
		Assert.assertThat(state.getHistoricalImportances(), IsNull.notNullValue());
		Assert.assertThat(state.getImportanceInfo().isSet(), IsEqual.equalTo(false));
		Assert.assertThat(state.getRemoteLinks(), IsNull.notNullValue());
		Assert.assertThat(state.getAccountInfo(), IsNull.notNullValue());
		Assert.assertThat(state.getMultisigLinks(), IsNull.notNullValue());
		Assert.assertThat(state.getSmartTileMap(), IsNull.notNullValue());
		Assert.assertThat(state.getHeight(), IsNull.nullValue());
	}

	//endregion

	//region copy

	@Test
	public void copyCopiesAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountState state = new AccountState(address);

		// Act:
		final AccountState copy = state.copy();

		// Assert:
		Assert.assertThat(copy.getAddress(), IsEqual.equalTo(address));
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
		Assert.assertThat(copyImportance, IsNot.not(IsSame.sameInstance(importance)));
		Assert.assertThat(importance.getImportance(height), IsEqual.equalTo(0.03125));
		Assert.assertThat(copyImportance.getImportance(new BlockHeight(234)), IsEqual.equalTo(0.0234375));
		Assert.assertThat(copyImportance.getOutlinksSize(height), IsEqual.equalTo(1));
		Assert.assertThat(
				copyImportance.getOutlinksIterator(height, height).next(),
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
		Assert.assertThat(copyBalances, IsNot.not(IsSame.sameInstance(balances)));
		Assert.assertThat(copyBalances.getUnvested(new BlockHeight(17)), IsEqual.equalTo(Amount.fromNem(1234)));
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
		Assert.assertThat(copyImportances, IsNot.not(IsSame.sameInstance(importances)));
		Assert.assertThat(copyImportances.getHistoricalImportance(new BlockHeight(17)), IsEqual.equalTo(0.3));
		Assert.assertThat(copyImportances.getHistoricalPageRank(new BlockHeight(17)), IsEqual.equalTo(0.6));
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
		Assert.assertThat(links.isEmpty(), IsEqual.equalTo(false));
		Assert.assertThat(copyLinks.isEmpty(), IsEqual.equalTo(true));
		Assert.assertThat(copyLinks.getCurrent(), IsNull.nullValue());
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
		Assert.assertThat(state.getAccountInfo().getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(copy.getAccountInfo().getBalance(), IsEqual.equalTo(Amount.fromNem(1000)));
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
		Assert.assertThat(stateLinks.isCosignatoryOf(multisig1), IsEqual.equalTo(true));
		Assert.assertThat(stateLinks.isCosignatoryOf(multisig2), IsEqual.equalTo(false));

		Assert.assertThat(copyLinks.isCosignatoryOf(multisig1), IsEqual.equalTo(true));
		Assert.assertThat(copyLinks.isCosignatoryOf(multisig2), IsEqual.equalTo(true));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfSmartTileMap() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final SmartTileMap originalSmartTileMap = state.getSmartTileMap();
		final MosaicId mosaicId1 = new MosaicId(new NamespaceId("foo"), "bar");
		final MosaicId mosaicId2 = new MosaicId(new NamespaceId("baz"), "qux");
		final SmartTile smartTile1 = new SmartTile(mosaicId1, Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(mosaicId2, Quantity.fromValue(234));
		originalSmartTileMap.add(smartTile1);

		// Act:
		final AccountState copy = state.copy();
		final SmartTileMap copyOfSmartTileMap = copy.getSmartTileMap();
		copyOfSmartTileMap.add(smartTile2);

		// Assert:
		Assert.assertThat(originalSmartTileMap.get(mosaicId1), IsEqual.equalTo(smartTile1));
		Assert.assertThat(originalSmartTileMap.get(mosaicId2), IsNull.nullValue());

		Assert.assertThat(copyOfSmartTileMap.get(mosaicId1), IsEqual.equalTo(smartTile1));
		Assert.assertThat(copyOfSmartTileMap.get(mosaicId2), IsEqual.equalTo(smartTile2));
	}

	@Test
	public void copyCopiesHeight() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		state.setHeight(new BlockHeight(17));

		// Act:
		final AccountState copy = state.copy();

		// Assert:
		Assert.assertThat(copy.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	//endregion

	//region height

	@Test
	public void accountHeightCanBeSetIfNull() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());

		// Act:
		state.setHeight(new BlockHeight(17));

		// Assert:
		Assert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	@Test
	public void accountHeightCannotBeUpdatedIfNonNull() {
		// Arrange:
		final AccountState state = new AccountState(Utils.generateRandomAddress());

		// Act:
		state.setHeight(new BlockHeight(17));
		state.setHeight(new BlockHeight(32));

		// Assert:
		Assert.assertThat(state.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	//endregion
}