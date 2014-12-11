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
 * A facade on top of the poi package.
 */
public class PoiFacade implements Iterable<PoiAccountState> {
	private final Map<Address, PoiAccountState> addressToStateMap = new ConcurrentHashMap<>();
	private final ImportanceCalculator importanceCalculator;
	private BlockHeight lastPoiRecalculationHeight;
	private int lastPoiVectorSize;

	/**
	 * Creates a new poi facade.
	 *
	 * @param importanceCalculator The importance calculator to use.
	 */
	public PoiFacade(final ImportanceCalculator importanceCalculator) {
		this.importanceCalculator = importanceCalculator;
	}

	/**
	 * Finds a poi account state given an address. This function will NOT return
	 * forwarded states.
	 *
	 * @param address The address.
	 * @return The poi account state.
	 */
	public PoiAccountState findStateByAddress(final Address address) {
		PoiAccountState state = this.addressToStateMap.getOrDefault(address, null);
		if (null == state) {
			state = new PoiAccountState(address);
			this.addressToStateMap.put(address, state);
		}

		return state;
	}

	/**
	 * Finds the latest poi account state given an address following all forwards.
	 * - When passed a remote harvester, it will return the state for the "owner" (the account harvesting remotely)
	 * - Otherwise, it will return the state for the passed in address
	 * <br/>
	 * Let's say we have account A and remote account B,
	 * A has link (B, height, HarvestingRemotely)
	 * B has link (A, height, RemoteHarvester)
	 * <br/>
	 * findForwardedStateByAddress(A *) should return A
	 * findForwardedStateByAddress(B, h+1439) should return B
	 * findForwardedStateByAddress(B, h+1440) should return B
	 * <br/>
	 *
	 * @param address The address.
	 * @return The poi account state.
	 */
	public PoiAccountState findLatestForwardedStateByAddress(final Address address) {
		final PoiAccountState state = this.findStateByAddress(address);
		final RemoteLinks remoteLinks = state.getRemoteLinks();
		final RemoteLink remoteLink = remoteLinks.getCurrent();
		return !remoteLinks.isRemoteHarvester() ? state : this.findStateByAddress(remoteLink.getLinkedAddress());
	}

	/**
	 * Finds a poi account state given an address following all forwards at a height.
	 * - When passed a remote harvester, it will return the state for the "owner" (the account harvesting remotely)
	 * - Otherwise, it will return the state for the passed in address
	 * <br/>
	 * Let's say we have account A and remote account B,
	 * A has link (B, height, HarvestingRemotely)
	 * B has link (A, height, RemoteHarvester)
	 * <br/>
	 * findForwardedStateByAddress(A *) should return A
	 * findForwardedStateByAddress(B, h+1439) should return B
	 * findForwardedStateByAddress(B, h+1440) should return A
	 * <br/>
	 *
	 * @param address The address.
	 * @param height Height at which check should be performed.
	 * @return The poi account state.
	 */
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

	/**
	 * Gets the number of account states.
	 *
	 * @return The number of account states.
	 */
	public int size() {
		return this.addressToStateMap.size();
	}

	/**
	 * Gets the size of the last poi vector (needed for time synchronization).
	 *
	 * @return The size of the last poi vector.
	 */
	public int getLastPoiVectorSize() {
		return this.lastPoiVectorSize;
	}

	/**
	 * Gets the height at which the last recalculation was (needed for time synchronization).
	 *
	 * @return The the height at which the last recalculation was.
	 */
	public BlockHeight getLastPoiRecalculationHeight() {
		return this.lastPoiRecalculationHeight;
	}

	/**
	 * Removes an account state from the cache if it is in the cache.
	 *
	 * @param address The address of the account state to remove.
	 */
	public void removeFromCache(final Address address) {
		this.addressToStateMap.remove(address);
	}

	/**
	 * Copies this facade's states to another facade's map.
	 *
	 * @param rhs The other facade.
	 */
	public void shallowCopyTo(final PoiFacade rhs) {
		rhs.addressToStateMap.clear();
		rhs.addressToStateMap.putAll(this.addressToStateMap);
		rhs.lastPoiRecalculationHeight = this.lastPoiRecalculationHeight;
	}

	/**
	 * Undoes weighted balances vesting to a given block height.
	 *
	 * @param height The block height.
	 */
	public void undoVesting(final BlockHeight height) {
		this.addressToStateMap.values().stream().forEach(a -> a.getWeightedBalances().undoChain(height));
	}

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 */
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

	/**
	 * Gets a debit predicate that checks balances against the account information stored in this cache.
	 *
	 * @return The debit predicate.
	 */
	public DebitPredicate getDebitPredicate() {
		return (account, amount) -> {
			final AccountInfo accountInfo = this.findStateByAddress(account.getAddress()).getAccountInfo();
			return accountInfo.getBalance().compareTo(amount) >= 0;
		};
	}

	/**
	 * Creates a copy of this repository.
	 *
	 * @return A copy of this repository.
	 */
	public PoiFacade copy() {
		final PoiFacade copy = new PoiFacade(this.importanceCalculator);
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
