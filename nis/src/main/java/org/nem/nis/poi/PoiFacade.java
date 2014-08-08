package org.nem.nis.poi;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A facade on top of the poi package.
 */
public class PoiFacade {
	private final Map<Address, PoiAccountState> addressToStateMap = new ConcurrentHashMap<>();
	private final PoiImportanceGenerator importanceGenerator;
	private BlockHeight lastPoiRecalculationHeight;

	/**
	 * Creates a new poi facade.
	 *
	 * @param importanceGenerator The importance generator to use.
	 */
	public PoiFacade(final PoiImportanceGenerator importanceGenerator) {
		this.importanceGenerator = importanceGenerator;
	}

	/**
	 * Finds a poi account state given an address.
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
	 * Gets the number of account states.
	 *
	 * @return The number of account states.
	 */
	public int size() {
		return this.addressToStateMap.size();
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
		if (null != this.lastPoiRecalculationHeight && 0 == this.lastPoiRecalculationHeight.compareTo(blockHeight))
			return;

		this.importanceGenerator.updateAccountImportances(blockHeight, this.getAccountStates(blockHeight));
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
	 * Creates a copy of this repository.
	 *
	 * @return A copy of this repository.
	 */
	public PoiFacade copy() {
		final PoiFacade copy = new PoiFacade(this.importanceGenerator);
		for (final Map.Entry<Address, PoiAccountState> entry : this.addressToStateMap.entrySet()) {
			copy.addressToStateMap.put(entry.getKey(), entry.getValue().copy());
		}

		copy.lastPoiRecalculationHeight = this.lastPoiRecalculationHeight;
		return copy;
	}
}
