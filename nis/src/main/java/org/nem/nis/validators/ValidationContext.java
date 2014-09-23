package org.nem.nis.validators;

import org.nem.core.model.primitive.BlockHeight;

/**
 * Contextual information associated with a validation.
 */
public class ValidationContext {
	private final BlockHeight blockHeight;
	private final DebitPredicate debitPredicate;

	/**
	 * Creates a default validation context.
	 */
	public ValidationContext() {
		this(BlockHeight.MAX);
	}

	/**
	 * Creates a validation context with a custom block height.
	 *
	 * @param blockHeight The block height.
	 */
	public ValidationContext(final BlockHeight blockHeight) {
		this(blockHeight, (account, amount) -> account.getBalance().compareTo(amount) >= 0);
	}

	/**
	 * Creates a validation context with a custom debit predicate.
	 *
	 * @param debitPredicate The debit predicate.
	 */
	public ValidationContext(final DebitPredicate debitPredicate) {
		this(BlockHeight.MAX, debitPredicate);
	}

	/**
	 * Creates a validation context with a custom block height and debit predicate.
	 *
	 * @param blockHeight The block height.
	 * @param debitPredicate The debit predicate.
	 */
	public ValidationContext(final BlockHeight blockHeight, final DebitPredicate debitPredicate) {
		this.blockHeight = blockHeight;
		this.debitPredicate = debitPredicate;
	}

	/**
	 * Gets the block height.
	 *
	 * @return The block height.
	 */
	public BlockHeight getBlockHeight() {
		return this.blockHeight;
	}

	/**
	 * Gets the debit predicate.
	 *
	 * @return The debit predicate.
	 */
	public DebitPredicate getDebitPredicate() {
		return this.debitPredicate;
	}
}
