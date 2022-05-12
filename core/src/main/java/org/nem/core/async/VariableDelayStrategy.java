package org.nem.core.async;

import java.util.function.Supplier;

/**
 * DelayStrategy that produces variable delays produced by an external supplier.
 */
public class VariableDelayStrategy extends AbstractDelayStrategy {

	private final Supplier<Integer> delaySupplier;

	/**
	 * Creates a new infinite variable delay strategy.
	 *
	 * @param delaySupplier The delay supplier.
	 */
	public VariableDelayStrategy(final Supplier<Integer> delaySupplier) {
		this.delaySupplier = delaySupplier;
	}

	/**
	 * Creates a new finite variable delay strategy.
	 *
	 * @param delaySupplier The delay supplier.
	 * @param maxDelays The maximum number of delays.
	 */
	public VariableDelayStrategy(final Supplier<Integer> delaySupplier, final int maxDelays) {
		super(maxDelays);
		this.delaySupplier = delaySupplier;
	}

	@Override
	protected int nextInternal(final int iteration) {
		return this.delaySupplier.get();
	}
}
