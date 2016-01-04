package org.nem.core.model.transactions.extensions;

import org.nem.core.model.Transaction;

import java.util.Collection;

/**
 * An aggregate transaction validation extension.
 *
 * @param <TTransaction> The type of transaction.
 */
public class AggregateTransactionValidationExtension<TTransaction extends Transaction> {
	private final Collection<TransactionValidationExtension<TTransaction>> extensions;

	/**
	 * Creates a new aggregate extension.
	 *
	 * @param extensions The child extensions.
	 */
	public AggregateTransactionValidationExtension(final Collection<TransactionValidationExtension<TTransaction>> extensions) {
		this.extensions = extensions;
	}

	/**
	 * Validates the specified transaction against all rules.
	 *
	 * @param transaction The transaction.
	 */
	public void validate(final TTransaction transaction) {
		this.extensions.stream()
				.filter(e -> e.isApplicable(transaction.getEntityVersion()))
				.forEach(e -> e.validate(transaction));
	}
}