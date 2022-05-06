package org.nem.nis.websocket;

import org.nem.core.model.Transaction;
import org.nem.core.model.ValidationResult;

public interface UnconfirmedTransactionListener {
	/**
	 * Publishes newly obtained transaction to UnconfirmedTransactionListener.
	 *
	 * @param transaction Unconfirmed transaction.
	 * @param validationResult Result of a validation, only successful transactions are published, so this should always be
	 *            ValidationResult.SUCCESS.
	 */
	void pushTransaction(final Transaction transaction, final ValidationResult validationResult);
}
