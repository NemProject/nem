package org.nem.nis.validators;

import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.*;

/**
 * Contextual information associated with a validation.
 */
public class ValidationContext {
	private final BlockHeight blockHeight;
	private final BlockHeight confirmedBlockHeight;
	private final DebitPredicate<Amount> xemDebitPredicate;
	private final DebitPredicate<Mosaic> mosaicDebitPredicate;

	/**
	 * Creates a validation context with a custom debit predicate.
	 *
	 * @param xemDebitPredicate The XEM debit predicate.
	 * @param mosaicDebitPredicate The mosaic debit predicate.
	 */
	public ValidationContext(final DebitPredicate<Amount> xemDebitPredicate, final DebitPredicate<Mosaic>mosaicDebitPredicate) {
		this(BlockHeight.MAX, BlockHeight.MAX, xemDebitPredicate, mosaicDebitPredicate);
	}

	/**
	 * Creates a validation context with a custom block height.
	 *
	 * @param xemDebitPredicate The XEM debit predicate.
	 * @param blockHeight The block height.
	 * @param mosaicDebitPredicate The mosaic debit predicate.
	 */
	public ValidationContext(final BlockHeight blockHeight, final DebitPredicate<Amount> xemDebitPredicate, final DebitPredicate<Mosaic> mosaicDebitPredicate) {
		this(blockHeight, blockHeight, xemDebitPredicate, mosaicDebitPredicate);
	}

	/**
	 * Creates a validation context with a custom block height and debit predicate.
	 *
	 * @param blockHeight The block height.
	 * @param confirmedBlockHeight The block height of common parent.
	 * @param xemDebitPredicate The XEM debit predicate.
	 * @param mosaicDebitPredicate The mosaic debit predicate.
	 */
	public ValidationContext(
			final BlockHeight blockHeight,
			final BlockHeight confirmedBlockHeight,
			final DebitPredicate<Amount> xemDebitPredicate,
			final DebitPredicate<Mosaic> mosaicDebitPredicate) {
		this.blockHeight = blockHeight;
		this.confirmedBlockHeight = confirmedBlockHeight;
		this.xemDebitPredicate = xemDebitPredicate;
		this.mosaicDebitPredicate = mosaicDebitPredicate;
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
	 * Gets the XEM debit predicate.
	 *
	 * @return The XEM debit predicate.
	 */
	public DebitPredicate<Amount> getXemDebitPredicate() {
		return this.xemDebitPredicate;
	}

	/**
	 * Gets the mosaic debit predicate.
	 *
	 * @return The mosaic debit predicate.
	 */
	public DebitPredicate<Mosaic> getMosaicDebitPredicate() {
		return this.mosaicDebitPredicate;
	}
}
