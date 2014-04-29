package org.nem.core.model;

/**
 * Interface to simulate money transfer from or to account.
 */
public interface NemTransferSimulate {
	/**
	 * Simulates transferring amount of NEMs from account
	 * @param account The account from which NEMs are send.
	 * @param amount The amount of NEMs.
	 * @return true if account has enough of funds
	 */
	boolean sub(Account account, Amount amount);

	/**
	 * Simulates transferring amount of NEMs to account
	 * @param account The account to which NEMs are send.
	 * @param amount The amount of NEMs.
	 */
	void add(Account account, Amount amount);
}
