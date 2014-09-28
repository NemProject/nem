package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating BlockValidator objects.
 */
public class BlockValidatorFactory {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 */
	public BlockValidatorFactory(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Creates a block validator.
	 *
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public BlockValidator create(final PoiFacade poiFacade) {
		final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();
		builder.add(new NonFutureEntityValidator(this.timeProvider));
		builder.add(new EligibleSignerBlockValidator(poiFacade));
		return builder.build();
	}
}
