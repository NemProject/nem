package org.nem.nis.pox.poi;

import org.nem.core.model.Address;

/**
 * A weighted link between a source account and another account.
 */
public class WeightedLink {

	private final double weight;
	private final Address otherAccountAddress;

	/**
	 * Creates a new weighted link.
	 *
	 * @param otherAccountAddress The address of the linked account.
	 * @param weight The weight.
	 */
	public WeightedLink(final Address otherAccountAddress, final double weight) {
		this.weight = weight;
		this.otherAccountAddress = otherAccountAddress;
	}

	/**
	 * Gets the weight.
	 *
	 * @return The weight.
	 */
	public double getWeight() {
		return this.weight;
	}

	/**
	 * Gets the address of the linked account.
	 *
	 * @return The address of the linked account.
	 */
	public Address getOtherAccountAddress() {
		return this.otherAccountAddress;
	}

	@Override
	public int hashCode() {
		return (int) this.getWeight() ^ this.otherAccountAddress.getEncoded().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof WeightedLink)) {
			return false;
		}

		final WeightedLink rhs = (WeightedLink) obj;
		return this.weight == rhs.weight && this.otherAccountAddress.equals(rhs.otherAccountAddress);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", this.weight, this.otherAccountAddress);
	}
}
