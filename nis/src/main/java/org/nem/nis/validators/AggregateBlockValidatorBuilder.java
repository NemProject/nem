package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;

/**
 * Builder for building an aggregate BlockValidator.
 */
public class AggregateBlockValidatorBuilder {
	private final List<BlockValidator> validators = new ArrayList<>();

	/**
	 * Adds a validator to the aggregate.
	 *
	 * @param validator The validator to add.
	 */
	public void add(final BlockValidator validator) {
		this.validators.add(validator);
	}

	/**
	 * Builds the aggregate validator.
	 *
	 * @return the aggregate validator.
	 */
	public BlockValidator build() {
		return new AggregateBlockValidator(this.validators);
	}

	private static class AggregateBlockValidator implements BlockValidator {
		private final List<BlockValidator> validators;

		public AggregateBlockValidator(final List<BlockValidator> validators) {
			this.validators = validators;
		}

		@Override
		public ValidationResult validate(final Block block) {
			boolean isNeutral = false;
			for (final BlockValidator validator : this.validators) {
				final ValidationResult result = validator.validate(block);
				if (ValidationResult.NEUTRAL == result) {
					isNeutral = true;
				} else if (ValidationResult.SUCCESS != result) {
					return result;
				}
			}

			return isNeutral ? ValidationResult.NEUTRAL : ValidationResult.SUCCESS;
		}
	}
}
