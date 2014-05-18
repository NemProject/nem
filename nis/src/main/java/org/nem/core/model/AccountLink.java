package org.nem.core.model;

/**
 * Class to link accounts for the POI calculation. TODO: this class needs to be
 * hooked up to the object model and updated based on transactions. Maybe create
 * an AccountLinks class to handle this?
 * 
 */
public class AccountLink implements Comparable<AccountLink> {

	/**
	 * The height at which the link was created.
	 */
	private BlockHeight height; 

	/**
	 * 
	 */
	private Amount amount;
	
	/**
	 * The account this link leads to.
	 */
	private Account otherAccount;

	/**
	 * @param strength
	 * @param otherAccount
	 */
	public AccountLink(BlockHeight height, Amount amount, Account otherAccount) {
		super();
		this.height = height;
		this.amount = amount;
		this.otherAccount = otherAccount;
	}

	/**
	 * Returns the height at which the links was created.
	 * 
	 * @return the height
	 */
	public BlockHeight getHeight() {
		return height;
	}

	/**
	 * @return the amount
	 */
	public Amount getAmount() {
		return amount;
	}

	/**
	 * @param strength
	 *            the strength to set
	 */
	public void setStrength(Amount amount) {
		this.amount = amount;
	}

	/**
	 * @return the otherAccount
	 */
	public Account getOtherAccount() {
		return otherAccount;
	}

	/**
	 * @param otherAccount
	 *            the otherAccount to set
	 */
	public void setOtherAccount(Account otherAccount) {
		this.otherAccount = otherAccount;
	}

	@Override
	public int compareTo(final AccountLink rhs) {
		int[] comparisonResults = new int[] {
				this.getHeight().compareTo(rhs.getHeight()),
				this.getAmount().compareTo(rhs.getAmount()),
				this.getOtherAccount().getAddress().getEncoded().compareTo(rhs.getOtherAccount().getAddress().getEncoded()),
		};

		for (int result : comparisonResults) {
			if (result != 0)
				return result;
		}

		return 0;
	}
}
