package org.nem.nis.validators;

import org.nem.core.model.primitive.*;

/**
 * Contextual information associated with a validation.
 */
public class ValidationContext {
	private final BlockHeight blockHeight;
	private final BlockHeight confirmedBlockHeight;
	private final DebitPredicate<Amount> debitPredicate;

	/**
	 * Creates a validation context with a custom debit predicate.
	 *
	 * @param debitPredicate The debit predicate.
	 */
	public ValidationContext(final DebitPredicate<Amount> debitPredicate) {
		this(BlockHeight.MAX, BlockHeight.MAX, debitPredicate);
	}

	/**
	 * Creates a validation context with a custom block height.
	 *
	 * @param debitPredicate The debit predicate.
	 * @param blockHeight The block height.
	 */
	public ValidationContext(final BlockHeight blockHeight, final DebitPredicate<Amount> debitPredicate) {
		this(blockHeight, blockHeight, debitPredicate);
	}

	/**
	 * Creates a validation context with a custom block height and debit predicate.
	 *
	 * @param blockHeight The block height.
	 * @param confirmedBlockHeight The block height of common parent.
	 * @param debitPredicate The debit predicate.
	 */
	public ValidationContext(
			final BlockHeight blockHeight,
			final BlockHeight confirmedBlockHeight,
			final DebitPredicate<Amount> debitPredicate) {
		this.blockHeight = blockHeight;
		this.confirmedBlockHeight = confirmedBlockHeight;
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
	 * Gets the confirmed block height (all transactions at or below this block height can be considered confirmed).
	 *
	 * @return The confirmed block height.
	 */
	public BlockHeight getConfirmedBlockHeight() {
		return this.confirmedBlockHeight;
	}

	/**
	 * Gets the debit predicate.
	 *
	 * @return The debit predicate.
	 */
	public DebitPredicate<Amount> getDebitPredicate() {
		return this.debitPredicate;
	}
}
