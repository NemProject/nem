package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.state.*;
import org.nem.nis.validators.DebitPredicate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultAccountStateCache implements AccountStateCache, CopyableCache<DefaultAccountStateCache> {
	private final Map<Address, AccountState> addressToStateMap = new ConcurrentHashMap<>();

	@Override
	public AccountState findStateByAddress(final Address address) {
		AccountState state = this.addressToStateMap.getOrDefault(address, null);
		if (null == state) {
			state = new AccountState(address);
			this.addressToStateMap.put(address, state);
		}

		return state;
	}

	@Override
	public AccountState findLatestForwardedStateByAddress(final Address address) {
		final AccountState state = this.findStateByAddress(address);
		final ReadOnlyRemoteLinks remoteLinks = state.getRemoteLinks();
		final RemoteLink remoteLink = remoteLinks.getCurrent();
		return !remoteLinks.isRemoteHarvester() ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
	}

	@Override
	public AccountState findForwardedStateByAddress(final Address address, final BlockHeight height) {
		final AccountState state = this.findStateByAddress(address);
		final ReadOnlyRemoteLinks remoteLinks = state.getRemoteLinks();
		if (!remoteLinks.isRemoteHarvester()) {
			return state;
		}

		final RemoteLink remoteLink = remoteLinks.getCurrent();
		final long settingHeight = height.subtract(remoteLink.getEffectiveHeight());
		boolean shouldUseRemote = false;
		switch (ImportanceTransferTransaction.Mode.fromValueOrDefault(remoteLink.getMode())) {
			case Activate:
				// the remote is active and operational
				shouldUseRemote = settingHeight >= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
				break;

			case Deactivate:
				// the remote hasn't been deactivated yet
				shouldUseRemote = settingHeight < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
				break;
		}

		return !shouldUseRemote ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
	}

	@Override
	public int size() {
		return this.addressToStateMap.size();
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
	public DebitPredicate getDebitPredicate() {
		return (account, amount) -> {
			final ReadOnlyAccountInfo accountInfo = this.findStateByAddress(account.getAddress()).getAccountInfo();
			return accountInfo.getBalance().compareTo(amount) >= 0;
		};
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

	// TODO: is this necessary?
	@Override
	public Iterator<AccountState> iterator() {
		return this.addressToStateMap.values().iterator();
	}
}
