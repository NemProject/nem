package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.PoiFacade;

import java.util.function.Consumer;

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
		this.visitSubValidators(builder::add, poiFacade);
		return builder.build();
	}

	/**
	 * Visits all sub validators that comprise the returned aggregate validator.
	 * @param poiFacade The poi facade.
	 *
	 * @param visitor The visitor.
	 */
	public void visitSubValidators(final Consumer<BlockValidator> visitor, final PoiFacade poiFacade) {
		visitor.accept(new NonFutureEntityValidator(this.timeProvider));
		visitor.accept(new TransactionDeadlineBlockValidator());
		visitor.accept(new EligibleSignerBlockValidator(poiFacade));
		visitor.accept(new MaxTransactionsBlockValidator());
		visitor.accept(new NoSelfSignedTransactionsBlockValidator(poiFacade));
		visitor.accept(new BlockImportanceTransferValidator());
		visitor.accept(new BlockImportanceTransferBalanceValidator());
	}
}
