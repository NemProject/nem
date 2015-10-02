package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultAccountStateCache implements ExtendedAccountStateCache<DefaultAccountStateCache> {
	private final Map<Address, AccountState> addressToStateMap = new ConcurrentHashMap<>();

	// the default behavior is to return a new (non-cached) AccountState so that validators can inspect
	// account states of all other accounts in a transaction (even if the other accounts are unknown)
	private final StateFinder stateFinder = new StateFinder(this.addressToStateMap, AccountState::new);

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
		return new CacheContents<>(this.addressToStateMap.values());
	}

	@Override
	public void removeFromCache(final Address address) {
		this.addressToStateMap.remove(address);
	}

	@Override
	public void undoVesting(final BlockHeight height) {
		this.addressToStateMap.values().stream().forEach(a -> a.getWeightedBalances().undoChain(height));
	}

	@Override
	public CacheContents<AccountState> mutableContents() {
		return new CacheContents<>(this.addressToStateMap.values());
	}

	public AccountStateCache asAutoCache() {
		return new DefaultAccountStateCacheAutoCache(this);
	}

	@Override
	public void shallowCopyTo(final DefaultAccountStateCache rhs) {
		rhs.addressToStateMap.clear();
		rhs.addressToStateMap.putAll(this.addressToStateMap);
	}

	@Override
	public DefaultAccountStateCache copy() {
		final DefaultAccountStateCache copy = new DefaultAccountStateCache();
		for (final Map.Entry<Address, AccountState> entry : this.addressToStateMap.entrySet()) {
			copy.addressToStateMap.put(entry.getKey(), entry.getValue().copy());
		}

		return copy;
	}

	private static class StateFinder {
		private final Map<Address, AccountState> addressToStateMap;
		private final Function<Address, AccountState> unknownAddressHandler;

		public StateFinder(
				final Map<Address, AccountState> addressToStateMap,
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
			}

			return !shouldUseRemote ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
		}
	}

	private static class DefaultAccountStateCacheAutoCache implements AccountStateCache {
		private final DefaultAccountStateCache impl;
		private final StateFinder stateFinder;

		public DefaultAccountStateCacheAutoCache(final DefaultAccountStateCache impl) {
			this.impl = impl;
			this.stateFinder = new StateFinder(this.impl.addressToStateMap, address -> {
				final AccountState state = new AccountState(address);
				this.impl.addressToStateMap.put(address, state);
				return state;
			});
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
			return this.impl.size();
		}

		@Override
		public void removeFromCache(final Address address) {
			this.impl.removeFromCache(address);
		}

		@Override
		public void undoVesting(final BlockHeight height) {
			this.impl.undoVesting(height);
		}

		@Override
		public CacheContents<AccountState> mutableContents() {
			return this.impl.mutableContents();
		}

		@Override
		public CacheContents<ReadOnlyAccountState> contents() {
			return this.impl.contents();
		}
	}
}
