package org.nem.nis.state;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;

/**
 * A link between a source account and another account. <br>
 * This class is immutable.
 */
public class AccountLink implements Comparable<AccountLink> {

	private final BlockHeight height;
	private final Amount amount;
	private final Address otherAccountAddress;

	/**
	 * Creates an account link.
	 *
	 * @param height The block height.
	 * @param amount The amount.
	 * @param otherAccountAddress The address of the linked account.
	 */
	public AccountLink(final BlockHeight height, final Amount amount, final Address otherAccountAddress) {
		this.height = height;
		this.amount = amount;
		this.otherAccountAddress = otherAccountAddress;
	}

	/**
	 * Gets the block height of this link.
	 *
	 * @return The block height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets the amount.
	 *
	 * @return The amount.
	 */
	public Amount getAmount() {
		return this.amount;
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
	public int compareTo(final AccountLink rhs) {
		final int[] comparisonResults = new int[]{
				this.getHeight().compareTo(rhs.getHeight()), this.getAmount().compareTo(rhs.getAmount()),
				this.otherAccountAddress.compareTo(rhs.otherAccountAddress),
		};

		for (final int result : comparisonResults) {
			if (0 != result) {
				return result;
			}
		}

		return 0;
	}

	@Override
	public int hashCode() {
		return this.getHeight().hashCode() ^ this.getAmount().hashCode() ^ this.otherAccountAddress.getEncoded().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof AccountLink && 0 == this.compareTo((AccountLink) obj);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s @ %s", this.getAmount(), this.otherAccountAddress, this.getHeight());
	}
}
