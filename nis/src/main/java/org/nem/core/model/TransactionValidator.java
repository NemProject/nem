package org.nem.core.model;

public interface TransactionValidator {
	public boolean validateTransfer(final Account sender, final Account recipient, final Amount amount);
}
