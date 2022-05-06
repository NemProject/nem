package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.validators.BlockValidator;

/**
 * A decorator that only executes a validator at or above a configured block height.
 */
public class BlockHeightBlockValidatorDecorator implements BlockValidator {
	private final BlockHeight effectiveBlockHeight;
	private final BlockValidator innerValidator;

	/**
	 * Creates a new validator decorator.
	 *
	 * @param effectiveBlockHeight The height at which the inner validator should be in effect.
	 * @param innerValidator The inner validator.
	 */
	public BlockHeightBlockValidatorDecorator(final BlockHeight effectiveBlockHeight, final BlockValidator innerValidator) {
		this.effectiveBlockHeight = effectiveBlockHeight;
		this.innerValidator = innerValidator;
	}

	@Override
	public String getName() {
		return String.format("%s @ %s", this.innerValidator.getName(), this.effectiveBlockHeight);
	}

	@Override
	public ValidationResult validate(final Block block) {
		return block.getHeight().compareTo(this.effectiveBlockHeight) < 0 ? ValidationResult.SUCCESS : this.innerValidator.validate(block);
	}
}
