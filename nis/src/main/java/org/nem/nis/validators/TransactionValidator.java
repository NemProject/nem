package org.nem.nis.validators;

/**
 * Interface for validating a single transaction or batches of transactions.
 */
public interface TransactionValidator extends SingleTransactionValidator, BatchTransactionValidator {
}
