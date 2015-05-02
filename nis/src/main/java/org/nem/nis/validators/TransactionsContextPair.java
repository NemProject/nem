package org.nem.nis.validators;

import org.nem.core.model.Transaction;

import java.util.*;

/**
 * A pair comprised of a validation context and a list of transactions.
 */
public class TransactionsContextPair {
	private final Collection<Transaction> transactions;
	private final ValidationContext context;

	/**
	 * Creates a transactions context pair around a single transaction.
	 *
	 * @param transaction The transaction.
	 * @param context The validation context.
	 */
	public TransactionsContextPair(final Transaction transaction, final ValidationContext context) {
		this(Collections.singletonList(transaction), context);
	}

	/**
	 * Creates a transactions context pair around multiple transactions.
	 *
	 * @param transactions The transactions.
	 * @param context The validation context.
	 */
	public TransactionsContextPair(final Collection<Transaction> transactions, final ValidationContext context) {
		this.transactions = transactions;
		this.context = context;
	}

	/**
	 * Gets the transactions.
	 *
	 * @return The transactions.
	 */
	public Collection<Transaction> getTransactions() {
		return this.transactions;
	}

	/**
	 * Gets the validation context.
	 *
	 * @return The context.
	 */
	public ValidationContext getContext() {
		return this.context;
	}
}
