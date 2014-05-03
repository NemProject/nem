package org.nem.core.model;

/**
 * Class to link accounts for the POI calculation. TODO: this class needs to be
 * hooked up to the object model and updated based on transactions. Maybe create
 * an AccountLinks class to handle this?
 * 
 */
public class AccountLink {
	double strength;// XXX: should be coinday weighted

	Account otherAccount; // TODO: this probably needs to be a hash/public key?

	/**
	 * 
	 */
	public AccountLink() {
		super();
	}

	/**
	 * @param strength
	 * @param otherAccount
	 */
	public AccountLink(double strength, Account otherAccount) {
		super();
		this.strength = strength;
		this.otherAccount = otherAccount;
	}

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
