package org.nem.core.model;

/**
 * Functional interface to simulate money transfer from or to account.
 */
public interface NemTransfer {
	void evaluate(Account account, Amount amount);
}
