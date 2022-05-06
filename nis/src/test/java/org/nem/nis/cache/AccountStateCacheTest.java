package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;
import org.nem.nis.test.NisTestConstants;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public abstract class AccountStateCacheTest<T extends ExtendedAccountStateCache<T>> {
	private static final ImportanceTransferMode UNKNOWN = ImportanceTransferMode.Unknown;
	private static final ImportanceTransferMode ACTIVATE = ImportanceTransferMode.Activate;
	private static final ImportanceTransferMode DEACTIVATE = ImportanceTransferMode.Deactivate;
	private static final int REMOTE_HARVESTING_DELAY = NisTestConstants.REMOTE_HARVESTING_DELAY;

	/**
	 * Creates a cache.
	 *
	 * @return The cache
	 */
	protected AccountStateCache createCache() {
		return this.createCacheWithoutAutoCache().copy();
	}

	/**
	 * Creates a cache that has auto-caching disabled.
	 *
	 * @return The cache
	 */
	protected abstract T createCacheWithoutAutoCache();

	// region findStateByAddress

	@Test
	public void findStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateCache cache = this.createCache();

		// Act:
		final AccountState state = cache.findStateByAddress(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(state, IsNull.notNullValue());
		MatcherAssert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findStateByAddressReturnsSameStateForSameAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		this.addToCache(cache.copy(), address);

		// Act:
		final AccountState state1 = cache.findStateByAddress(address);
		final AccountState state2 = cache.findStateByAddress(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(state2, IsEqual.equalTo(state1));
	}

	@Test
	public void findStateByAddressFailsForInvalidAddress() {
		// Assert:
		this.assertFunctionFailsForInvalidAddress((address, cache) -> cache.findStateByAddress(address));
	}

	@Test
	public void findStateByAddressDoesNotCacheStateForUnknownAddressInNonAutoCacheMode() {
		// Act:
		this.assertFunctionDoesNotCacheStateForUnknownAddressInNonAutoCachedMode((address, cache) -> cache.findStateByAddress(address));
	}

	@Test
	public void findStateByAddressSucceedsForKnownAddressInNonAutoCacheMode() {
		// Assert:
		this.assertFunctionSucceedsForKnownAddressInNonAutoCachedMode((address, cache) -> cache.findStateByAddress(address));
	}

	// endregion

	// region findLatestForwardedStateByAddress

	@Test
	public void findLatestForwardedStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateCache cache = this.createCache();

		// Act:
		final AccountState state = cache.findLatestForwardedStateByAddress(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(state, IsNull.notNullValue());
		MatcherAssert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findLatestForwardedStateByAddressReturnsLocalStateWhenAccountIsHarvestingRemotely() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		MatcherAssert.assertThat(this.isLatestLocalState(ACTIVATE, 1, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLatestLocalState(ACTIVATE, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findLatestForwardedStateByAddressReturnsRemoteStateWhenAccountIsRemoteHarvester() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		MatcherAssert.assertThat(this.isLatestLocalState(ACTIVATE, 1, owner), IsEqual.equalTo(false));
		MatcherAssert.assertThat(this.isLatestLocalState(ACTIVATE, 1000, owner), IsEqual.equalTo(false));
	}

	private boolean isLatestLocalState(final ImportanceTransferMode mode, final int remoteBlockHeight, final RemoteLink.Owner owner) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		final AccountState state = this.addToCache(cache.copy(), address);
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(remoteBlockHeight), mode, owner);
		state.getRemoteLinks().addLink(link);

		// Act:
		final AccountState forwardedState = cache.findLatestForwardedStateByAddress(address);

		// Assert:
		return forwardedState.equals(state);
	}

	@Test
	public void findLatestForwardedStateByAddressFailsForInvalidAddress() {
		// Assert:
		this.assertFunctionFailsForInvalidAddress((address, cache) -> cache.findLatestForwardedStateByAddress(address));
	}

	@Test
	public void findLatestForwardedStateByAddressDoesNotCacheStateForUnknownAddressInNonAutoCacheMode() {
		// Act:
		this.assertFunctionDoesNotCacheStateForUnknownAddressInNonAutoCachedMode(
				(address, cache) -> cache.findLatestForwardedStateByAddress(address));
	}

	@Test
	public void findLatestForwardedStateByAddressSucceedsForKnownAddressInNonAutoCacheMode() {
		// Assert:
		this.assertFunctionSucceedsForKnownAddressInNonAutoCachedMode((address, cache) -> cache.findLatestForwardedStateByAddress(address));
	}

	// endregion

	// region findForwardedStateByAddress

	@Test
	public void findForwardedStateByAddressReturnsStateForAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateCache cache = this.createCache();

		// Act:
		final AccountState state = cache.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(state, IsNull.notNullValue());
		MatcherAssert.assertThat(state.getAddress(), IsEqual.equalTo(address));
	}

	@Test
	public void findForwardedStateByAddressReturnsSameStateForSameAddress() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		this.addToCache(cache.copy(), address);

		// Act:
		final AccountState state1 = cache.findForwardedStateByAddress(address, BlockHeight.ONE);
		final AccountState state2 = cache.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(state2, IsEqual.equalTo(state1));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateWhenAccountDoesNotHaveRemoteState() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		final AccountState state = this.addToCache(cache.copy(), address);

		// Act:
		final AccountState forwardedState = cache.findForwardedStateByAddress(address, BlockHeight.ONE);

		// Assert:
		MatcherAssert.assertThat(forwardedState, IsEqual.equalTo(state));
	}

	@Test
	public void findForwardedStateByAddressFailsForInvalidAddress() {
		// Assert:
		this.assertFunctionFailsForInvalidAddress((address, cache) -> cache.findForwardedStateByAddress(address, BlockHeight.ONE));
	}

	@Test
	public void findForwardedStateByAddressDoesNotCacheStateForUnknownAddressInNonAutoCacheMode() {
		// Act:
		this.assertFunctionDoesNotCacheStateForUnknownAddressInNonAutoCachedMode(
				(address, cache) -> cache.findForwardedStateByAddress(address, BlockHeight.ONE));
	}

	@Test
	public void findForwardedStateByAddressSucceedsForKnownAddressInNonAutoCacheMode() {
		// Assert:
		this.assertFunctionSucceedsForKnownAddressInNonAutoCachedMode(
				(address, cache) -> cache.findForwardedStateByAddress(address, BlockHeight.ONE));
	}

	// endregion

	// region common helpers

	private void assertFunctionFailsForInvalidAddress(final BiFunction<Address, T, AccountState> findState) {
		// Arrange:
		final Address address = Address.fromEncoded("bad");
		final T cache = this.createCacheWithoutAutoCache();

		// Act:
		ExceptionAssert.assertThrows(v -> findState.apply(address, cache), MissingResourceException.class);
	}

	private void assertFunctionDoesNotCacheStateForUnknownAddressInNonAutoCachedMode(final BiFunction<Address, T, AccountState> findState) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();

		// Act:
		final AccountState state = findState.apply(address, cache);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(state, IsNull.notNullValue());
	}

	private void assertFunctionSucceedsForKnownAddressInNonAutoCachedMode(final BiFunction<Address, T, AccountState> findState) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		final AccountState state1 = this.addToCache(cache.copy(), address);

		// Act:
		final AccountState state2 = findState.apply(address, cache);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(state2, IsNull.notNullValue());
		MatcherAssert.assertThat(state2.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(state2, IsSame.sameInstance(state1));
	}

	// endregion

	// region HarvestingRemotely

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenActiveRemoteIsAgedAtLeastRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1, REMOTE_HARVESTING_DELAY + 1, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1000, 2880, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenActiveRemoteIsAgedLessThanRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1, REMOTE_HARVESTING_DELAY, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenInactiveRemoteIsAgedLessThanRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1, REMOTE_HARVESTING_DELAY, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenInactiveRemoteIsAgedAtLeastRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1, REMOTE_HARVESTING_DELAY + 1, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1000, REMOTE_HARVESTING_DELAY + 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForHarvestingRemotelyWhenRemoteModeIsUnknown() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.HarvestingRemotely;
		MatcherAssert.assertThat(this.isLocalState(UNKNOWN, 1, REMOTE_HARVESTING_DELAY + 1, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(UNKNOWN, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	// endregion

	// region RemoteHarvester

	@Test
	public void findForwardedStateByAddressReturnsForwardedStateForRemoteHarvesterWhenActiveRemoteIsAgedAtLeastRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1, REMOTE_HARVESTING_DELAY + 1, owner), IsEqual.equalTo(false));
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1000, REMOTE_HARVESTING_DELAY + 1000, owner), IsEqual.equalTo(false));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForRemoteHarvesterWhenActiveRemoteIsAgedLessThanRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1, REMOTE_HARVESTING_DELAY, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(ACTIVATE, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsForwardedStateForRemoteHarvesterWhenInactiveRemoteIsAgedLessThanRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1, REMOTE_HARVESTING_DELAY, owner), IsEqual.equalTo(false));
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1000, 1000, owner), IsEqual.equalTo(false));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForRemoteHarvesterWhenInactiveRemoteIsAgedAtLeastRemoteHarvestingDelayLimitBlocks() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1, REMOTE_HARVESTING_DELAY + 1, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(DEACTIVATE, 1000, REMOTE_HARVESTING_DELAY + 1000, owner), IsEqual.equalTo(true));
	}

	@Test
	public void findForwardedStateByAddressReturnsLocalStateForRemoteHarvesterWhenRemoteModeIsUnknown() {
		// Assert:
		final RemoteLink.Owner owner = RemoteLink.Owner.RemoteHarvester;
		MatcherAssert.assertThat(this.isLocalState(UNKNOWN, 1, REMOTE_HARVESTING_DELAY + 1, owner), IsEqual.equalTo(true));
		MatcherAssert.assertThat(this.isLocalState(UNKNOWN, 1000, 1000, owner), IsEqual.equalTo(true));
	}

	// endregion

	private boolean isLocalState(final ImportanceTransferMode mode, final int remoteBlockHeight, final int currentBlockHeight,
			final RemoteLink.Owner owner) {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		final AccountState state = this.addToCache(cache.copy(), address);
		final RemoteLink link = new RemoteLink(Utils.generateRandomAddress(), new BlockHeight(remoteBlockHeight), mode, owner);
		state.getRemoteLinks().addLink(link);

		// Act:
		final AccountState forwardedState = cache.findForwardedStateByAddress(address, new BlockHeight(currentBlockHeight));

		// Assert:
		return forwardedState.equals(state);
	}

	// endregion

	// region removeFromCache

	@Test
	public void accountWithoutPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountStateCache cache = this.createCache();

		// Act:
		cache.findStateByAddress(address);
		cache.removeFromCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void accountWithPublicKeyCanBeRemovedFromCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountStateCache cache = this.createCache();

		// Act:
		cache.findStateByAddress(address);
		cache.removeFromCache(address);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void removeAccountFromCacheDoesNothingIfAddressIsNotInCache() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountStateCache cache = this.createCache();

		// Act:
		cache.findStateByAddress(address);
		cache.removeFromCache(Utils.generateRandomAddress());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region copy

	@Test
	public void copyCreatesUnlinkedAccountStateCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();

		final T cache = this.createCacheWithoutAutoCache();
		final AccountState state1 = this.addToCache(cache.copy(), address1);
		final AccountState state2 = this.addToCache(cache.copy(), address2);
		final AccountState state3 = this.addToCache(cache.copy(), address3);

		// Act:
		final T copy = cache.copy();

		final AccountState copyState1 = copy.findStateByAddress(address1);
		final AccountState copyState2 = copy.findStateByAddress(address2);
		final AccountState copyState3 = copy.findStateByAddress(address3);

		// Assert:
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(3));
		assertEquivalentButNotSame(copyState1, state1);
		assertEquivalentButNotSame(copyState2, state2);
		assertEquivalentButNotSame(copyState3, state3);
	}

	private static void assertEquivalentButNotSame(final AccountState lhs, final AccountState rhs) {
		MatcherAssert.assertThat(lhs, IsNot.not(IsSame.sameInstance(rhs)));
		MatcherAssert.assertThat(lhs.getAddress(), IsEqual.equalTo(rhs.getAddress()));
	}

	@Test
	public void copyReturnsSameAccountGivenPublicKeyOrAddress() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();
		this.addToCache(cache.copy(), address1);

		// Act:
		final AccountStateCache copyCache = cache.copy();

		final AccountState copyStateFromEncoded = copyCache.findStateByAddress(Address.fromEncoded(address1.getEncoded()));
		final AccountState copyStateFromPublicKey = copyCache.findStateByAddress(address1);

		// Assert:
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copyStateFromEncoded, IsSame.sameInstance(copyStateFromPublicKey));
	}

	// endregion

	// region shallowCopyTo

	@Test
	public void shallowCopyToCreatesLinkedCacheCopy() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final Address address3 = Utils.generateRandomAddress();

		final T cache = this.createCacheWithoutAutoCache();
		final AccountState state1 = this.addToCache(cache.copy(), address1);
		final AccountState state2 = this.addToCache(cache.copy(), address2);
		final AccountState state3 = this.addToCache(cache.copy(), address3);

		// Act:
		final T copyCache = this.createCacheWithoutAutoCache();
		cache.shallowCopyTo(copyCache);

		final AccountState copyState1 = copyCache.findStateByAddress(address1);
		final AccountState copyState2 = copyCache.findStateByAddress(address2);
		final AccountState copyState3 = copyCache.findStateByAddress(address3);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(copyState1, IsSame.sameInstance(state1));
		MatcherAssert.assertThat(copyState2, IsSame.sameInstance(state2));
		MatcherAssert.assertThat(copyState3, IsSame.sameInstance(state3));
	}

	@Test
	public void shallowCopyToRemovesAnyPreviouslyExistingEntries() {
		// Arrange:
		final Address address1 = Utils.generateRandomAddress();
		final Address address2 = Utils.generateRandomAddress();
		final T cache = this.createCacheWithoutAutoCache();

		final AccountState state1 = this.addToCache(cache.copy(), address1);

		final T copyCache = this.createCacheWithoutAutoCache();
		final AccountState state2 = this.addToCache(copyCache.copy(), address2);

		// Act:
		cache.shallowCopyTo(copyCache);

		final AccountState copyState1 = copyCache.findStateByAddress(address1);
		final AccountState copyState2 = copyCache.findStateByAddress(address2);

		// Assert:
		MatcherAssert.assertThat(copyCache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copyState1, IsSame.sameInstance(state1));
		MatcherAssert.assertThat(copyState2, IsNot.not(IsSame.sameInstance(state2))); // note that copyState2 is created on access
	}

	// endregion

	// region undoVesting

	@Test
	public void undoVestingDelegatesToWeightedBalances() {
		// Arrange:
		final T cache = this.createCacheWithoutAutoCache().copy();
		final List<AccountState> accountStates = createAccountStatesForUndoVestingTests(3, cache);

		// Expect: all accounts should have two weighted balance entries
		for (final AccountState accountState : accountStates) {
			MatcherAssert.assertThat(accountState.getWeightedBalances().size(), IsEqual.equalTo(2));
		}

		// Act:
		cache.undoVesting(new BlockHeight(7));
		cache.commit();

		// Assert: one weighted balance entry should have been removed from all accounts
		for (final ReadOnlyAccountState accountState : cache.contents().asCollection()) {
			MatcherAssert.assertThat(accountState.getWeightedBalances().size(), IsEqual.equalTo(1));
		}
	}

	private static List<AccountState> createAccountStatesForUndoVestingTests(final int numAccounts, final AccountStateCache cache) {
		final List<AccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < numAccounts; ++i) {
			accountStates.add(cache.findStateByAddress(Utils.generateRandomAddress()));
			accountStates.get(i).getWeightedBalances().addFullyVested(new BlockHeight(7), Amount.fromNem(i + 1));
			accountStates.get(i).getWeightedBalances().addFullyVested(new BlockHeight(8), Amount.fromNem(2 * (i + 1)));
		}

		return accountStates;
	}

	// endregion

	// region contents / mutable contents

	@Test
	public void contentsReturnsAllAccounts() {
		// Assert:
		this.assertContentsReturnsAllAccounts(ReadOnlyAccountStateCache::contents);
	}

	@Test
	public void mutableContentsReturnsAllAccounts() {
		// Assert:
		this.assertContentsReturnsAllAccounts(AccountStateCache::mutableContents);
	}

	private void assertContentsReturnsAllAccounts(
			final Function<AccountStateCache, CacheContents<? extends ReadOnlyAccountState>> toContents) {
		// Arrange:
		final AccountStateCache cache = this.createCache();

		final List<AccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < 3; ++i) {
			accountStates.add(cache.findStateByAddress(Utils.generateRandomAddress()));
		}

		// Act:
		final Collection<? extends ReadOnlyAccountState> contents = toContents.apply(cache).asCollection();

		// Assert:
		MatcherAssert.assertThat(contents.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(contents.stream().map(ReadOnlyAccountState::getAddress).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(accountStates.stream().map(AccountState::getAddress).collect(Collectors.toList())));
	}

	// endregion

	private AccountState addToCache(final T cache, final Address address) {
		// note: this is necessary in order for the state getting copied to the copiedValues map
		final AccountState state = cache.findStateByAddress(address);
		cache.commit();
		return state;
	}
}
