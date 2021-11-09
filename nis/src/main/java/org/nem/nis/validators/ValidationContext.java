package org.nem.nis.validators;

import org.nem.core.model.primitive.BlockHeight;

/**
 * Contextual information associated with a validation.
 */
public class ValidationContext {
	private final BlockHeight blockHeight;
	private final BlockHeight confirmedBlockHeight;
	private final ValidationState state;

	/**
	 * Creates a validation context with a custom validation state.
	 *
	 * @param state The validation state.
	 */
	public ValidationContext(final ValidationState state) {
		this(BlockHeight.MAX, BlockHeight.MAX, state);
	}

	/**
	 * Creates a validation context with a custom block height.
	 *
	 * @param blockHeight The block height.
	 * @param state The validation state.
	 */
	public ValidationContext(final BlockHeight blockHeight, final ValidationState state) {
		this(blockHeight, blockHeight, state);
	}

	/**
	 * Creates a validation context with a custom block height and debit predicate.
	 *
	 * @param blockHeight The block height.
	 * @param confirmedBlockHeight The block height of common parent.
	 * @param state The validation state.
	 */
	public ValidationContext(final BlockHeight blockHeight, final BlockHeight confirmedBlockHeight, final ValidationState state) {
		this.blockHeight = blockHeight;
		this.confirmedBlockHeight = confirmedBlockHeight;
		this.state = state;
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
	 * Gets the validation state.
	 *
	 * @return The validation state.
	 */
	public ValidationState getState() {
		return this.state;
	}
}
