package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.poi.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.DebitPredicate;

import java.util.*;
import java.util.stream.*;

public abstract class AccountStateRepositoryTest<T extends CopyableCache<T> & AccountStateRepository> {

	/**
	 * Creates a cache.
	 *
	 * @return The cache
	 */
	protected abstract T createCache();

	//region findStateByAddress

	@Test
	public void findStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();

		// Act:
		final AccountState state = facade.findStateByAddress(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state, IsNull.notNullValue());
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findStateByAddressReturnsSameStateForSameAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();

		// Act:
		final AccountState state1 = facade.findStateByAddress(address);
		final AccountState state2 = facade.findStateByAddress(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state2, IsEqual.equalTo(state1));
	}

	//endregion

	//region findLatestForwardedStateByAddress

	@Test
	public void findLatestForwardedStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();

		// Act:
		final AccountState state = facade.findLatestForwardedStateByAddress(address);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state, IsNull.notNullValue());
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findLatestForwardedStateByAddressReturnsLocalStateWhenAccountIsHarvestingRemotely() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		Assert.assertThat(isLatestLocalState(1, 1, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLatestLocalState(1, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findLatestForwardedStateByAddressReturnsRemoteStateWhenAccountIsRemoteHarvester() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		Assert.assertThat(isLatestLocalState(1, 1, owner), IsEqual.equalTo(false));
		Assert.assertThat(isLatestLocalState(1, 1000, owner), IsEqual.equalTo(false));
	}

	private boolean isLatestLocalState(final int mode, final int remoteBlockHeight, final RemoteLink.Owner owner) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();
		final AccountState state = facade.findStateByAddress(address);
		final RemoteLink link = new RemoteLink(
				Utils.generateRandomAddress(),
				new BlockHeight(remoteBlockHeight),
				mode,
				owner);
		state.getRemoteLinks().addLink(link);

		// Act:
		final AccountState forwardedState = facade.findLatestForwardedStateByAddress(address);

		// Assert:
		return forwardedState.equals(state);
	}

	//endregion

	//region findForwardedStateByAddress

	@Test
	public void findForwardedStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();

		// Act:
		final AccountState state = facade.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state, IsNull.notNullValue());
		Assert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findForwardedStateByAddressReturnsSameStateForSameAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();

		// Act:
		final AccountState state1 = facade.findForwardedStateByAddress(address, BlockHeight.ONE);
		final AccountState state2 = facade.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(1));
		Assert.assertThat(state2, IsEqual.equalTo(state1));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenAccountDoesNotHaveRemoteState() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();
		final AccountState state = facade.findStateByAddress(address);

		// Act:
		final AccountState forwardedState = facade.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(forwardedState, IsEqual.equalTo(state));
	}

	//region HarvestingRemotely

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenActiveRemoteIsAgedAtLeastOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		Assert.assertThat(isLocalState(1, 1, 1441, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(1, 1000, 2880, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenActiveRemoteIsAgedLessThanOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		Assert.assertThat(isLocalState(1, 1, 1440, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(1, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenInactiveRemoteIsAgedLessThanOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		Assert.assertThat(isLocalState(2, 1, 1440, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(2, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenInactiveRemoteIsAgedAtLeastOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		Assert.assertThat(isLocalState(2, 1, 1441, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(2, 1000, 2880, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenRemoteModeIsUnknown() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		Assert.assertThat(isLocalState(7, 1, 1441, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(0, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	//endregion

	//region RemoteHarvester

	@Test
	public void findForwardedStateByAddressReturnsForwardedStateForRemoteHarvesterWhenActiveRemoteIsAgedAtLeastOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		Assert.assertThat(isLocalState(1, 1, 1441, owner), IsEqual.equalTo(false));
		Assert.assertThat(isLocalState(1, 1000, 2880, owner), IsEqual.equalTo(false));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForRemoteHarvesterWhenActiveRemoteIsAgedLessThanOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		Assert.assertThat(isLocalState(1, 1, 1440, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(1, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsForwardedStateForRemoteHarvesterWhenInactiveRemoteIsAgedLessThanOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		Assert.assertThat(isLocalState(2, 1, 1440, owner), IsEqual.equalTo(false));
		Assert.assertThat(isLocalState(2, 1000, 1000, owner), IsEqual.equalTo(false));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForRemoteHarvesterWhenInactiveRemoteIsAgedAtLeastOneDay() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		Assert.assertThat(isLocalState(2, 1, 1441, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(2, 1000, 2880, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForRemoteHarvesterWhenRemoteModeIsUnknown() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		Assert.assertThat(isLocalState(7, 1, 1441, owner), IsEqual.equalTo(true));
		Assert.assertThat(isLocalState(0, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	//endregion

	private boolean isLocalState(final int mode, final int remoteBlockHeight, final int currentBlockHeight, final RemoteLink.Owner owner) {
		// Assert:		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();
		final AccountState state = facade.findStateByAddress(address);
		final RemoteLink link = new RemoteLink(
				Utils.generateRandomAddress(),
				new BlockHeight(remoteBlockHeight),
				mode,
				owner);
		state.getRemoteLinks().addLink(link);

		// Act:
		final AccountState forwardedState = facade.findForwardedStateByAddress(address, new BlockHeight(currentBlockHeight));

		// Assert:
		return forwardedState.equals(state);
	}

	//endregion

	//region removeFromCache

	@Test
	public void accountWithoutPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateRepository facade = this.createCache();

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
		final AccountStateRepository facade = this.createCache();

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
		final AccountStateRepository facade = this.createCache();

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
		final T facade = this.createCache();

		final AccountState state1 = facade.findStateByAddress(address1);
		final AccountState state2 = facade.findStateByAddress(address2);
		final AccountState state3 = facade.findStateByAddress(address3);

		// Act:
		final T copyFacade = facade.copy();

		final AccountState copyState1 = copyFacade.findStateByAddress(address1);
		final AccountState copyState2 = copyFacade.findStateByAddress(address2);
		final AccountState copyState3 = copyFacade.findStateByAddress(address3);

		// Assert:
		Assert.assertThat(copyFacade.size(), IsEqual.equalTo(3));
		assertEquivalentButNotSame(copyState1, state1);
		assertEquivalentButNotSame(copyState2, state2);
		assertEquivalentButNotSame(copyState3, state3);
	}

	private static void assertEquivalentButNotSame(final AccountState lhs, final AccountState rhs) {
		Assert.assertThat(lhs, IsNot.not(IsSame.sameInstance(rhs)));
		Assert.assertThat(lhs.getAddress(), IsEqual.equalTo(rhs.getAddress()));
	}

	@Test
	public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final T facade = this.createCache();

		facade.findStateByAddress(address1);

		// Act:
		final AccountStateRepository copyFacade = facade.copy();

		final AccountState copyStateFromEncoded = copyFacade.findStateByAddress(Address.fromEncoded(address1.getEncoded()));
		final AccountState copyStateFromPublicKey = copyFacade.findStateByAddress(address1);

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
		final T facade = this.createCache();

		final AccountState state1 = facade.findStateByAddress(address1);
		final AccountState state2 = facade.findStateByAddress(address2);
		final AccountState state3 = facade.findStateByAddress(address3);

		// Act:
		final T copyFacade = this.createCache();
		facade.shallowCopyTo(copyFacade);

		final AccountState copyState1 = copyFacade.findStateByAddress(address1);
		final AccountState copyState2 = copyFacade.findStateByAddress(address2);
		final AccountState copyState3 = copyFacade.findStateByAddress(address3);

		// Assert:
		Assert.assertThat(facade.size(), IsEqual.equalTo(3));
		Assert.assertThat(copyState1, IsSame.sameInstance(state1));
		Assert.assertThat(copyState2, IsSame.sameInstance(state2));
		Assert.assertThat(copyState3, IsSame.sameInstance(state3));
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final T facade = this.createCache();

		final AccountState state1 = facade.findStateByAddress(address1);

		final T copyFacade = this.createCache();
		final AccountState state2 = copyFacade.findStateByAddress(address2);

		// Act:
		facade.shallowCopyTo(copyFacade);

		final AccountState copyState1 = copyFacade.findStateByAddress(address1);
		final AccountState copyState2 = copyFacade.findStateByAddress(address2);

		// Assert:
		Assert.assertThat(copyFacade.size(), IsEqual.equalTo(2)); // note that copyState2 is created on access
		Assert.assertThat(copyState1, IsSame.sameInstance(state1));
		Assert.assertThat(copyState2, IsNot.not(IsSame.sameInstance(state2)));
	}

	//endregion

	//region undoVesting

	@Test
	public void undoVestingDelegatesToWeightedBalances() {
		// Arrange:
		final AccountStateRepository facade = this.createCache();
		final List<AccountState> accountStates = createAccountStatesForUndoVestingTests(3, facade);

		// Expect: all accounts should have two weighted balance entries
		for (final AccountState accountState : accountStates) {
			Assert.assertThat(accountState.getWeightedBalances().size(), IsEqual.equalTo(2));
		}

		// Act:
		facade.undoVesting(new BlockHeight(7));

		// Assert: one weighted balance entry should have been removed from all accounts
		for (final AccountState accountState : accountStates) {
			Assert.assertThat(accountState.getWeightedBalances().size(), IsEqual.equalTo(1));
		}
	}

	private static List<AccountState> createAccountStatesForUndoVestingTests(final int numAccounts, final AccountStateRepository facade) {
		final List<AccountState> accountStates = new ArrayList<>();
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
		final AccountStateRepository facade = this.createCache();

		final List<AccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			accountStates.add(facade.findStateByAddress(Utils.generateRandomAddress()));
		}

		// Act:
		final List<AccountState> iteratedAccountStates = StreamSupport.stream(facade.spliterator(), false)
				.collect(Collectors.toList());

		// Assert:
		Assert.assertThat(iteratedAccountStates.size(), IsEqual.equalTo(3));
		Assert.assertThat(
				iteratedAccountStates.stream().map(AccountState::getAddress).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(accountStates.stream().map(AccountState::getAddress).collect(Collectors.toList())));
	}

	//endregion

	//region getDebitPredicate

	@Test
	public void getDebitPredicateEvaluatesAmountAgainstBalancesInAccountState() {
		// Arrange:
		final AccountStateRepository accountStateRepository = this.createCache();
		final Account account1 = addAccountWithBalance(accountStateRepository, Amount.fromNem(10));
		final Account account2 = addAccountWithBalance(accountStateRepository, Amount.fromNem(77));

		// Act:
		final DebitPredicate debitPredicate = accountStateRepository.getDebitPredicate();

		// Assert:
		Assert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(9)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(10)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account1, Amount.fromNem(11)), IsEqual.equalTo(false));

		Assert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(76)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(77)), IsEqual.equalTo(true));
		Assert.assertThat(debitPredicate.canDebit(account2, Amount.fromNem(78)), IsEqual.equalTo(false));
	}

	private static Account addAccountWithBalance(final AccountStateRepository accountStateRepository, final Amount amount) {
		final Account account = Utils.generateRandomAccount();
		final AccountState accountState = accountStateRepository.findStateByAddress(account.getAddress());
		accountState.getAccountInfo().incrementBalance(amount);
		return account;
	}

	//endregion
}