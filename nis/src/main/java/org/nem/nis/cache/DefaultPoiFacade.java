package org.nem.nis.cache;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.poi.*;
import org.nem.nis.remote.*;
import org.nem.nis.validators.DebitPredicate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A repository of all mutable NEM account state.
 */
public class DefaultPoiFacade implements PoiFacade, CopyableCache<DefaultPoiFacade> {
	private final Map<Address, PoiAccountState> addressToStateMap = new ConcurrentHashMap<>();
	private final ImportanceCalculator importanceCalculator;
	private BlockHeight lastPoiRecalculationHeight;
	private int lastPoiVectorSize;

	/**
	 * Creates a new poi facade.
	 *
	 * @param importanceCalculator The importance calculator to use.
	 */
	public DefaultPoiFacade(final ImportanceCalculator importanceCalculator) {
		this.importanceCalculator = importanceCalculator;
	}

	@Override
	public PoiAccountState findStateByAddress(final Address address) {
		PoiAccountState state = this.addressToStateMap.getOrDefault(address, null);
		if (null == state) {
			state = new PoiAccountState(address);
			this.addressToStateMap.put(address, state);
		}

		return state;
	}

	@Override
	public PoiAccountState findLatestForwardedStateByAddress(final Address address) {
		final PoiAccountState state = this.findStateByAddress(address);
		final RemoteLinks remoteLinks = state.getRemoteLinks();
		final RemoteLink remoteLink = remoteLinks.getCurrent();
		return !remoteLinks.isRemoteHarvester() ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
	}

	@Override
	public PoiAccountState findForwardedStateByAddress(final Address address, final BlockHeight height) {
		final PoiAccountState state = this.findStateByAddress(address);
		final RemoteLinks remoteLinks = state.getRemoteLinks();
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
	public int getLastPoiVectorSize() {
		return this.lastPoiVectorSize;
	}

	@Override
	public BlockHeight getLastPoiRecalculationHeight() {
		return this.lastPoiRecalculationHeight;
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
	public void recalculateImportances(final BlockHeight blockHeight) {
		if (null != this.lastPoiRecalculationHeight && 0 == this.lastPoiRecalculationHeight.compareTo(blockHeight)) {
			return;
		}

		final Collection<PoiAccountState> accountStates = this.getAccountStates(blockHeight);
		this.lastPoiVectorSize = accountStates.size();
		this.importanceCalculator.recalculate(blockHeight, accountStates);
		this.lastPoiRecalculationHeight = blockHeight;
	}

	private Collection<PoiAccountState> getAccountStates(final BlockHeight blockHeight) {
		return this.addressToStateMap.values().stream()
				.filter(a -> shouldIncludeInImportanceCalculation(a, blockHeight))
				.collect(Collectors.toList());
	}

	private static boolean shouldIncludeInImportanceCalculation(final PoiAccountState accountState, final BlockHeight blockHeight) {
		return null != accountState.getHeight()
				&& accountState.getHeight().compareTo(blockHeight) <= 0
				&& !accountState.getAddress().equals(NemesisBlock.ADDRESS);
	}

	@Override
	public DebitPredicate getDebitPredicate() {
		return (account, amount) -> {
			final AccountInfo accountInfo = this.findStateByAddress(account.getAddress()).getAccountInfo();
			return accountInfo.getBalance().compareTo(amount) >= 0;
		};
	}

	@Override
	public void shallowCopyTo(final DefaultPoiFacade rhs) {
		rhs.addressToStateMap.clear();
		rhs.addressToStateMap.putAll(this.addressToStateMap);
		rhs.lastPoiRecalculationHeight = this.lastPoiRecalculationHeight;
	}

	@Override
	public DefaultPoiFacade copy() {
		final DefaultPoiFacade copy = new DefaultPoiFacade(this.importanceCalculator);
		for (final Map.Entry<Address, PoiAccountState> entry : this.addressToStateMap.entrySet()) {
			copy.addressToStateMap.put(entry.getKey(), entry.getValue().copy());
		}

		copy.lastPoiRecalculationHeight = this.lastPoiRecalculationHeight;
		return copy;
	}

	@Override
	public Iterator<PoiAccountState> iterator() {
		return this.addressToStateMap.values().iterator();
	}
}
