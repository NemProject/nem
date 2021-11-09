package org.nem.nis.validators.transaction;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.validators.*;

/**
 * A decorator that only executes a validator at or above a configured block height.
 */
public class BlockHeightSingleTransactionValidatorDecorator implements SingleTransactionValidator {
	private final BlockHeight effectiveBlockHeight;
	private final SingleTransactionValidator innerValidator;

	/**
	 * Creates a new validator decorator.
	 *
	 * @param effectiveBlockHeight The height at which the inner validator should be in effect.
	 * @param innerValidator The inner validator.
	 */
	public BlockHeightSingleTransactionValidatorDecorator(final BlockHeight effectiveBlockHeight,
			final SingleTransactionValidator innerValidator) {
		this.effectiveBlockHeight = effectiveBlockHeight;
		this.innerValidator = innerValidator;
	}

	@Override
	public String getName() {
		return String.format("%s @ %s", this.innerValidator.getName(), this.effectiveBlockHeight);
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return context.getBlockHeight().compareTo(this.effectiveBlockHeight) < 0
				? ValidationResult.SUCCESS
				: this.innerValidator.validate(transaction, context);
	}
}
