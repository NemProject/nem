package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;

import java.util.*;

/**
 * A mapping of mosaic balances.
 */
public class MosaicBalances implements ReadOnlyMosaicBalances {
	private final Map<Address, Quantity> balances = new HashMap<>();

	//region ReadOnlyMosaicBalances

	@Override
	public int size() {
		return this.balances.size();
	}

	@Override
	public Quantity getBalance(final Address address) {
		return this.balances.getOrDefault(address, Quantity.ZERO);
	}

	//endregion

	/**
	 * Increments the balance of the specified account.
	 *
	 * @param address The account address.
	 * @param amount The amount.
	 */
	public void incrementBalance(final Address address, final Quantity amount) {
		this.balances.put(address, this.getBalance(address).add(amount));
	}

	/**
	 * Decrements the balance of the specified account.
	 *
	 * @param address The account address.
	 * @param amount The amount.
	 */
	public void decrementBalance(final Address address, final Quantity amount) {
		final Quantity newQuantity = this.getBalance(address).subtract(amount);
		if (Quantity.ZERO.equals(newQuantity)) {
			this.balances.remove(address);
		} else {
			this.balances.put(address, this.getBalance(address).subtract(amount));
		}
	}

	/**
	 * Creates a copy of this MosaicBalances.
	 *
	 * @return The copy.
	 */
	public MosaicBalances copy() {
		// addresses and quantities are immutable
		final MosaicBalances copy = new MosaicBalances();
		copy.balances.putAll(this.balances);
		return copy;
	}
}
