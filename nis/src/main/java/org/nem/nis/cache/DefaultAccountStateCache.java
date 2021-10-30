package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.delta.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultAccountStateCache implements ExtendedAccountStateCache<DefaultAccountStateCache> {
	private final MutableObjectAwareDeltaMap<Address, AccountState> addressToStateMap;
	private boolean isCopy = false;

	// the default behavior is to return a new (non-cached) AccountState so that validators can inspect
	// account states of all other accounts in a transaction (even if the other accounts are unknown)
	private final StateFinder stateFinder;

	/**
	 * Creates an account state cache.
	 */
	public DefaultAccountStateCache() {
		this(new MutableObjectAwareDeltaMap<>(2048));
	}

	private DefaultAccountStateCache(final MutableObjectAwareDeltaMap<Address, AccountState> addressToStateMap) {
		this(addressToStateMap, AccountState::new);
	}

	private DefaultAccountStateCache(final MutableObjectAwareDeltaMap<Address, AccountState> addressToStateMap,
			final Function<Address, AccountState> unknownAddressHandler) {
		this.addressToStateMap = addressToStateMap;
		this.stateFinder = new StateFinder(this.addressToStateMap, unknownAddressHandler);
	}

	@Override
	public AccountState findStateByAddress(final Address address) {
		return this.stateFinder.findStateByAddress(address);
	}

	@Override
	public AccountState findLatestForwardedStateByAddress(final Address address) {
		return this.stateFinder.findLatestForwardedStateByAddress(address);
	}

	@Override
	public AccountState findForwardedStateByAddress(final Address address, final BlockHeight height) {
		return this.stateFinder.findForwardedStateByAddress(address, height);
	}

	@Override
	public int size() {
		return this.addressToStateMap.size();
	}

	@Override
	public CacheContents<ReadOnlyAccountState> contents() {
		return new CacheContents<>(
				this.addressToStateMap.readOnlyEntrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
	}

	@Override
	public void removeFromCache(final Address address) {
		this.addressToStateMap.remove(address);
	}

	@Override
	public void undoVesting(final BlockHeight height) {
		// TODO 20151008 J-B: so, i think the real issue isn't that we *need* a copy here ...
		// > i think the weighted balances need to be delta-aware too
		// > ofc, we don't really need this for the mijin network where we're always vesting
		this.addressToStateMap.streamValues().forEach(a -> a.getWeightedBalances().undoChain(height));
	}

	@Override
	public CacheContents<AccountState> mutableContents() {
		return new CacheContents<>(this.addressToStateMap.streamValues().collect(Collectors.toList()));
	}

	@Override
	public void shallowCopyTo(final DefaultAccountStateCache rhs) {
		this.addressToStateMap.shallowCopyTo(rhs.addressToStateMap);
	}

	@Override
	public DefaultAccountStateCache copy() {
		if (this.isCopy) {
			// TODO 20151013 J-J: add test for this case
			throw new IllegalStateException("nested copies are currently not allowed");
		}

		// note that this is not copying at all.
		final MutableObjectAwareDeltaMap<Address, AccountState> rebasedDeltaMap = this.addressToStateMap.rebase();
		final DefaultAccountStateCache copy = new DefaultAccountStateCache(rebasedDeltaMap, address -> {
			final AccountState state = new AccountState(address);
			rebasedDeltaMap.put(address, state);
			return state;
		});
		copy.isCopy = true;
		return copy;
	}

	@Override
	public void commit() {
		// TODO 20151013 J-J: add test for commit
		this.addressToStateMap.commit();
	}

	/**
	 * Creates a deep copy of this account state cache.
	 *
	 * @return The deep copy.
	 */
	public DefaultAccountStateCache deepCopy() {
		// TODO 20151013 J-J: add test for deepCopy
		return new DefaultAccountStateCache(this.addressToStateMap.deepCopy());
	}

	private static class StateFinder {
		private final DeltaMap<Address, AccountState> addressToStateMap;
		private final Function<Address, AccountState> unknownAddressHandler;

		public StateFinder(final DeltaMap<Address, AccountState> addressToStateMap,
				final Function<Address, AccountState> unknownAddressHandler) {
			this.addressToStateMap = addressToStateMap;
			this.unknownAddressHandler = unknownAddressHandler;
		}

		public AccountState findStateByAddress(final Address address) {
			if (!address.isValid()) {
				throw new MissingResourceException("invalid address", Address.class.getName(), address.toString());
			}

			final AccountState state = this.addressToStateMap.getOrDefault(address, null);
			if (null != state) {
				return state;
			}

			return this.unknownAddressHandler.apply(address);
		}

		public AccountState findLatestForwardedStateByAddress(final Address address) {
			final AccountState state = this.findStateByAddress(address);
			final ReadOnlyRemoteLinks remoteLinks = state.getRemoteLinks();
			final RemoteLink remoteLink = remoteLinks.getCurrent();
			return !remoteLinks.isRemoteHarvester() ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
		}

		public AccountState findForwardedStateByAddress(final Address address, final BlockHeight height) {
			final AccountState state = this.findStateByAddress(address);
			final ReadOnlyRemoteLinks remoteLinks = state.getRemoteLinks();
			if (!remoteLinks.isRemoteHarvester()) {
				return state;
			}

			final RemoteLink remoteLink = remoteLinks.getCurrent();
			final long settingHeight = height.subtract(remoteLink.getEffectiveHeight());
			final int remoteHarvestingDelay = NemGlobals.getBlockChainConfiguration().getBlockChainRewriteLimit();
			boolean shouldUseRemote = false;
			switch (remoteLink.getMode()) {
				case Activate:
					// the remote is active and operational
					shouldUseRemote = settingHeight >= remoteHarvestingDelay;
					break;

				case Deactivate:
					// the remote hasn't been deactivated yet
					shouldUseRemote = settingHeight < remoteHarvestingDelay;
					break;
				case Unknown:
					break;
			}

			return !shouldUseRemote ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
		}
	}
}
