package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.Quantity;

import java.util.*;

/**
 * A mapping of mosaic balances.
 */
public class MosaicBalances implements ReadOnlyMosaicBalances {
	private final Map<Address, Quantity> balances = new HashMap<>();

	// region ReadOnlyMosaicBalances

	@Override
	public int size() {
		return this.balances.size();
	}

	@Override
	public Quantity getBalance(final Address address) {
		return this.balances.getOrDefault(address, Quantity.ZERO);
	}

	@Override
	public Collection<Address> getOwners() {
		return Collections.unmodifiableCollection(this.balances.keySet());
	}

	// endregion

	/**
	 * Increments the balance of the specified account.
	 *
	 * @param address The account address.
	 * @param increase The increase.
	 */
	public void incrementBalance(final Address address, final Quantity increase) {
		this.update(address, this.getBalance(address).add(increase));
	}

	/**
	 * Decrements the balance of the specified account.
	 *
	 * @param address The account address.
	 * @param decrease The decrease.
	 */
	public void decrementBalance(final Address address, final Quantity decrease) {
		this.update(address, this.getBalance(address).subtract(decrease));
	}

	private void update(final Address address, final Quantity amount) {
		if (Quantity.ZERO.equals(amount)) {
			this.balances.remove(address);
		} else {
			this.balances.put(address, amount);
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
