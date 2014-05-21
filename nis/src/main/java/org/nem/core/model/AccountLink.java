package org.nem.core.model;

/**
 * A link between a source account and another account.
 */
public class AccountLink implements Comparable<AccountLink> {

	private final BlockHeight height;
	private final Amount amount;
	private final Account otherAccount;

	/**
	 * Creates an account link.
	 *
	 * @param height The block height.
	 * @param amount The amount.
	 * @param otherAccount The linked account.
	 */
	public AccountLink(final BlockHeight height, final Amount amount, final Account otherAccount) {
		this.height = height;
		this.amount = amount;
		this.otherAccount = otherAccount;
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
	 * Gets the other account.
	 *
	 * @return The other account.
	 */
	public Account getOtherAccount() {
		return this.otherAccount;
	}

	@Override
	public int compareTo(final AccountLink rhs) {
		int[] comparisonResults = new int[] {
				this.getHeight().compareTo(rhs.getHeight()),
				this.getAmount().compareTo(rhs.getAmount()),
				this.getOtherAccount().getAddress().getEncoded().compareTo(rhs.getOtherAccount().getAddress().getEncoded()),
		};

		for (int result : comparisonResults) {
			if (0 != result)
				return result;
		}

		return 0;
	}

	@Override
	public int hashCode() {
		return this.getHeight().hashCode() ^
				this.getAmount().hashCode() ^
				this.getOtherAccount().getAddress().getEncoded().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof AccountLink && 0 == this.compareTo((AccountLink) obj);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s @ %s", this.getAmount(), this.getOtherAccount().getAddress(), this.getHeight());
	}
}
