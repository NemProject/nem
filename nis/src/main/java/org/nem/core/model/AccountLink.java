package org.nem.core.model;

/**
 * Class to link accounts for the POI calculation.
 * 
 */
public class AccountLink {
	double strength;

	Account otherAccount;

	/**
	 * @return the strength
	 */
	public double getStrength() {
		return strength;
	}

	/**
	 * @param strength
	 *            the strength to set
	 */
	public void setStrength(double strength) {
		this.strength = strength;
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
}
