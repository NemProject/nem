package org.nem.nis.validators.block;

import org.nem.core.model.*;
import org.nem.nis.validators.*;

import java.util.*;
import java.util.stream.Collectors;

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
		public String getName() {
			return this.validators.stream().map(NamedValidator::getName).collect(Collectors.joining(","));
		}

		@Override
		public ValidationResult validate(final Block block) {
			final Iterator<ValidationResult> resultIterator = this.validators.stream().map(validator -> validator.validate(block))
					.iterator();
			return ValidationResult.aggregate(resultIterator);
		}
	}
}
