package org.nem.nis.poi;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.*;

public class PoiFacadeTest {

	//region findStateByAddress

	@Test
	public void findStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		// Act:
		final PoiAccountState state = facade.findStateByAddress(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state, IsNull.notNullValue());
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findStateByAddressReturnsSameStateForSameAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		// Act:
		final PoiAccountState state1 = facade.findStateByAddress(address);
		final PoiAccountState state2 = facade.findStateByAddress(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state2, IsEqual.equalTo(state1));
	}

	//endregion

	//region findForwardedStateByAddress

	@Test
	public void findForwardedStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		// Act:
		final PoiAccountState state = facade.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state, IsNull.notNullValue());
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findForwardedStateByAddressReturnsSameStateForSameAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		// Act:
		final PoiAccountState state1 = facade.findForwardedStateByAddress(address, BlockHeight.ONE);
		final PoiAccountState state2 = facade.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state2, IsEqual.equalTo(state1));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenAccountDoesNotHaveRemoteState() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();
		final PoiAccountState state = facade.findStateByAddress(address);

		// Act:
		final PoiAccountState forwardedState = facade.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(forwardedState, IsEqual.equalTo(state));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenAccountIsRemoteHarvester() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();
		final PoiAccountState state = facade.findStateByAddress(address);
		state.getRemoteLinks().addLink(new RemoteLink(Utils.generateRandomAddress(), BlockHeight.ONE, 1, RemoteLink.Owner.RemoteHarvester));

		// Act:
		final PoiAccountState forwardedState = facade.findForwardedStateByAddress(address, new BlockHeight(2880));

		// Assert:
		Assert.assertThat(forwardedState, IsEqual.equalTo(state));
	}

	@Test
	public void findForwardedStateByAddressReturnsRemoteStateWhenActiveRemoteIsAgedAtLeastOneDay() {
		// Assert:
		Assert.assertThat(isLocalState(1, 1, 1441), IsEqual.equalTo(false));
		Assert.assertThat(isLocalState(1, 1000, 2880), IsEqual.equalTo(false));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenActiveRemoteIsAgedLessThanOneDay() {
		// Assert:
		Assert.assertThat(isLocalState(1, 1, 1440), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(1, 1000, 1000), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsRemoteStateWhenInactiveRemoteIsAgedLessThanOneDay() {
		// Assert:
		Assert.assertThat(isLocalState(2, 1, 1440), IsEqual.equalTo(false));
		Assert.assertThat(isLocalState(2, 1000, 1000), IsEqual.equalTo(false));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenInactiveRemoteIsAgedAtLeastOneDay() {
		// Assert:
		Assert.assertThat(isLocalState(2, 1, 1441), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(2, 1000, 2880), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenRemoteModeIsUnknown() {
		// Assert:
		Assert.assertThat(isLocalState(7, 1, 1441), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(0, 1000, 1000), IsEqual.equalTo(true));
	}

	private static boolean isLocalState(final int mode, final int remoteBlockHeight, final int currentBlockHeight) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();
		final PoiAccountState state = facade.findStateByAddress(address);
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(remoteBlockHeight), mode, RemoteLink.Owner.HarvestingRemotely);
		state.getRemoteLinks().addLink(link);

		// Act:
		final PoiAccountState forwardedState = facade.findForwardedStateByAddress(address, new BlockHeight(currentBlockHeight));

		// Assert:
		return forwardedState.equals(state);
	}

	//endregion

	//region removeFromCache

	@Test
	public void accountWithoutPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		// Act:
		facade.findStateByAddress(address);
		facade.removeFromCache(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(0));
	}

	@Test
	public void accountWithPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final PoiFacade facade = createPoiFacade();

		// Act:
		facade.findStateByAddress(address);
		facade.removeFromCache(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(0));
	}

	@Test
	public void removeAccountFromCacheDoesNothingIfAddressIsNotInCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final PoiFacade facade = createPoiFacade();

		// Act:
		facade.findStateByAddress(address);
		facade.removeFromCache(Utils.generateRandomAddress());

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
	}

	//endregion

	//region copy

	@Test
	public void copyCreatesUnlinkedFacadeCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		final PoiAccountState state1 = facade.findStateByAddress(address1);
		final PoiAccountState state2 = facade.findStateByAddress(address2);
		final PoiAccountState state3 = facade.findStateByAddress(address3);

		// Act:
		final PoiFacade copyFacade = facade.copy();

		final PoiAccountState copyState1 = copyFacade.findStateByAddress(address1);
		final PoiAccountState copyState2 = copyFacade.findStateByAddress(address2);
		final PoiAccountState copyState3 = copyFacade.findStateByAddress(address3);

		// Assert:
		Assert.assertThat(copyFacade.size(), IsEqual.equalTo(3));
		assertEquivalentButNotSame(copyState1, state1);
		assertEquivalentButNotSame(copyState2, state2);
		assertEquivalentButNotSame(copyState3, state3);
	}

	private static void assertEquivalentButNotSame(final PoiAccountState lhs, final PoiAccountState rhs) {
		Assert.assertThat(lhs, IsNot.not(IsSame.sameInstance(rhs)));
		Assert.assertThat(lhs.getAddress(), IsEqual.equalTo(rhs.getAddress()));
	}

	@Test
	public void copyDoesNotRecalculateImportancesForSameBlock() {
		// Arrange:
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);

		final PoiFacade facade = new PoiFacade(importanceGenerator);
		facade.recalculateImportances(new BlockHeight(1234));

		// Act:
		final PoiFacade copyFacade = facade.copy();

		copyFacade.recalculateImportances(new BlockHeight(1234));

		// Assert: updateAccountImportances was only called once because the copy is using the cached result from the original
		Mockito.verify(importanceGenerator, Mockito.times(1)).updateAccountImportances(Mockito.any(), Mockito.any());
	}

	@Test
	public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		facade.findStateByAddress(address1);

		// Act:
		final PoiFacade copyFacade = facade.copy();

		final PoiAccountState copyStateFromEncoded = copyFacade.findStateByAddress(Address.fromEncoded(address1.getEncoded()));
		final PoiAccountState copyStateFromPublicKey = copyFacade.findStateByAddress(address1);

		// Assert:
		Assert.assertThat(copyFacade.size(), IsEqual.equalTo(1));
		Assert.assertThat(copyStateFromEncoded, IsSame.sameInstance(copyStateFromPublicKey));
	}

	//endregion

	//region shallowCopyTo

	@Test
	public void shallowCopyToCreatesLinkedAnalyzerCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		final PoiAccountState state1 = facade.findStateByAddress(address1);
		final PoiAccountState state2 = facade.findStateByAddress(address2);
		final PoiAccountState state3 = facade.findStateByAddress(address3);

		// Act:
		final PoiFacade copyFacade = createPoiFacade();
		facade.shallowCopyTo(copyFacade);

		final PoiAccountState copyState1 = copyFacade.findStateByAddress(address1);
		final PoiAccountState copyState2 = copyFacade.findStateByAddress(address2);
		final PoiAccountState copyState3 = copyFacade.findStateByAddress(address3);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyState1, IsSame.sameInstance(state1));
		Assert.assertThat(copyState2, IsSame.sameInstance(state2));
		Assert.assertThat(copyState3, IsSame.sameInstance(state3));
	}

	@Test
	public void shallowCopyDoesNotRecalculateImportancesForSameBlock() {
		// Arrange:
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final PoiFacade facade = new PoiFacade(importanceGenerator);
		facade.recalculateImportances(new BlockHeight(1234));

		// Act:
		final PoiFacade copyFacade = createPoiFacade();
		facade.shallowCopyTo(copyFacade);

		facade.recalculateImportances(new BlockHeight(1234));

		// Assert: updateAccountImportances was only called once because the copy is using the cached result from the original
		Mockito.verify(importanceGenerator, Mockito.times(1)).updateAccountImportances(Mockito.any(), Mockito.any());
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final PoiFacade facade = createPoiFacade();

		final PoiAccountState state1 = facade.findStateByAddress(address1);

		final PoiFacade copyFacade = createPoiFacade();
		final PoiAccountState state2 = copyFacade.findStateByAddress(address2);

		// Act:
		facade.shallowCopyTo(copyFacade);

		final PoiAccountState copyState1 = copyFacade.findStateByAddress(address1);
		final PoiAccountState copyState2 = copyFacade.findStateByAddress(address2);

		// Assert:
		Assert.assertThat(copyFacade.size(), IsEqual.equalTo(2)); // note that copyState2 is created on access
		Assert.assertThat(copyState1, IsSame.sameInstance(state1));
		Assert.assertThat(copyState2, IsNot.not(IsSame.sameInstance(state2)));
	}

	//endregion

	//region recalculateImportances

	@Test
	public void recalculateImportancesDelegatesToImportanceGenerator() {
		// Arrange:
		final BlockHeight height = new BlockHeight(70);
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final PoiFacade facade = new PoiFacade(importanceGenerator);
		createAccountStatesForRecalculateTests(3, facade);

		// Act:
		facade.recalculateImportances(height);

		// Assert: the generator was called once and passed a collection with three accounts
		final ArgumentCaptor<Collection<PoiAccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceGenerator, Mockito.times(1)).updateAccountImportances(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L)));
	}

	@Test
	public void recalculateImportancesIgnoresAccountsWithGreaterHeight() {
		// Arrange:
		final BlockHeight height = new BlockHeight(20);
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final PoiFacade facade = new PoiFacade(importanceGenerator);
		createAccountStatesForRecalculateTests(3, facade);

		// Act:
		facade.recalculateImportances(height);

		// Assert: the generator was called once and passed a collection with two accounts
		final ArgumentCaptor<Collection<PoiAccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceGenerator, Mockito.times(1)).updateAccountImportances(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L)));
	}

	@Test
	public void recalculateImportancesIgnoresNemesisAccount() {
		// Arrange:
		final BlockHeight height = new BlockHeight(70);
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final PoiFacade facade = new PoiFacade(importanceGenerator);
		createAccountStatesForRecalculateTests(3, facade);
		facade.findStateByAddress(NemesisBlock.ADDRESS);

		// Act:
		facade.recalculateImportances(height);

		// Assert: the generator was called once and passed a collection with three accounts (but not the nemesis account)
		final ArgumentCaptor<Collection<PoiAccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceGenerator, Mockito.times(1)).updateAccountImportances(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L)));
	}

	@Test
	public void recalculateImportancesDoesNotRecalculateImportancesForLastBlockHeight() {
		// Arrange:
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);
		final PoiFacade facade = new PoiFacade(importanceGenerator);

		// Act:
		facade.recalculateImportances(new BlockHeight(7));
		facade.recalculateImportances(new BlockHeight(7));

		// Assert: the generator was only called once
		Mockito.verify(importanceGenerator, Mockito.times(1)).updateAccountImportances(Mockito.any(), Mockito.any());
	}

	@Test
	public void recalculateImportancesRecalculatesImportancesForNewBlockHeight() {
		// Arrange:
		final int height1 = 70;
		final int height2 = 80;
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);

		final PoiFacade facade = new PoiFacade(importanceGenerator);
		createAccountStatesForRecalculateTests(3, facade);

		// Act:
		facade.recalculateImportances(new BlockHeight(height1));
		facade.recalculateImportances(new BlockHeight(height2));

		// Assert: the generator was called twice and passed a collection with three accounts
		final ArgumentCaptor<Collection<PoiAccountState>> argument = createAccountStateCollectionArgumentCaptor();
		Mockito.verify(importanceGenerator, Mockito.times(2)).updateAccountImportances(Mockito.any(), argument.capture());
		Assert.assertThat(this.heightsAsList(argument.getValue()), IsEquivalent.equivalentTo(Arrays.asList(10L, 20L, 30L)));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiVectorSize() {
		// Arrange:
		final int height1 = 70;
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);

		final PoiFacade facade = new PoiFacade(importanceGenerator);
		createAccountStatesForRecalculateTests(3, facade);

		// Act:
		facade.recalculateImportances(new BlockHeight(height1));

		// Assert:
		Assert.assertThat(facade.getLastPoiVectorSize(), IsEqual.equalTo(3));
	}

	@Test
	public void recalculateImportancesUpdatesLastPoiRecalculationHeight() {
		// Arrange:
		final int height1 = 70;
		final PoiImportanceGenerator importanceGenerator = Mockito.mock(PoiImportanceGenerator.class);

		final PoiFacade facade = new PoiFacade(importanceGenerator);
		createAccountStatesForRecalculateTests(3, facade);

		// Act:
		facade.recalculateImportances(new BlockHeight(height1));

		// Assert:
		Assert.assertThat(facade.getLastPoiRecalculationHeight(), IsEqual.equalTo(new BlockHeight(70)));
	}

	private static List<PoiAccountState> createAccountStatesForRecalculateTests(final int numAccounts, final PoiFacade facade) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < numAccounts; ++i) {
			accountStates.add(facade.findStateByAddress(Utils.generateRandomAddress()));
			accountStates.get(i).setHeight(new BlockHeight((i + 1) * 10));
		}

		return accountStates;
	}

	private List<Long> heightsAsList(final Collection<PoiAccountState> accountStates) {
		return accountStates.stream()
				.map(as -> as.getHeight().getRaw())
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Collection<PoiAccountState>> createAccountStateCollectionArgumentCaptor() {
		return ArgumentCaptor.forClass((Class)Collection.class);
	}

	//endregion

	//region undoVesting

	@Test
	public void undoVestingDelegatesToWeightedBalances() {
		// Arrange:
		final PoiFacade facade = createPoiFacade();
		final List<PoiAccountState> accountStates = createAccountStatesForUndoVestingTests(3, facade);

		// Expect: all accounts should have two weighted balance entries
		for (final PoiAccountState accountState : accountStates) {
			Assert.assertThat(accountState.getWeightedBalances().size(), IsEqual.equalTo(2));
		}

		// Act:
		facade.undoVesting(new BlockHeight(7));

		// Assert: one weighted balance entry should have been removed from all accounts
		for (final PoiAccountState accountState : accountStates) {
			Assert.assertThat(accountState.getWeightedBalances().size(), IsEqual.equalTo(1));
		}
	}

	private static List<PoiAccountState> createAccountStatesForUndoVestingTests(final int numAccounts, final PoiFacade facade) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < numAccounts; ++i) {
			accountStates.add(facade.findStateByAddress(Utils.generateRandomAddress()));
			accountStates.get(i).getWeightedBalances().addFullyVested(new BlockHeight(7), Amount.fromNem(i + 1));
			accountStates.get(i).getWeightedBalances().addFullyVested(new BlockHeight(8), Amount.fromNem(2 * (i + 1)));
		}

		return accountStates;
	}

	//endregion

	//region iterator

	@Test
	public void iteratorReturnsAllAccounts() {
		// Arrange:
		final PoiFacade facade = createPoiFacade();

		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			accountStates.add(facade.findStateByAddress(Utils.generateRandomAddress()));
		}

		// Act:
		final List<PoiAccountState> iteratedAccountStates = StreamSupport.stream(facade.spliterator(), false)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(iteratedAccountStates.size(), IsEqual.equalTo(3));
		Assert.assertThat(
				iteratedAccountStates.stream().map(PoiAccountState::getAddress).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(accountStates.stream().map(PoiAccountState::getAddress).collect(Collectors.toList())));
	}

	//endregion

	private static PoiFacade createPoiFacade() {
		return new PoiFacade(Mockito.mock(PoiImportanceGenerator.class));
	}
}