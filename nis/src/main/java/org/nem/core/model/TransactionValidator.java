package org.nem.core.model;

public interface TransactionValidator {
	public boolean validateTransfer(final Account signer, final Account recipient, final Amount amount);
}
