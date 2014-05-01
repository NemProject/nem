package org.nem.core.async;

/**
 * DelayStrategy that linearly increases a delay from a minimum value
 * to a maximum value.
 */
public class LinearDelayStrategy extends AbstractDelayStrategy {

	private final int minDelay;
	private final float delayStep;

	/**
	 * Creates a new infinite uniform delay strategy.
	 *
	 * @param minDelay The minimum delay.
	 * @param maxDelay The maximum delay.
	 * @param iterations The number of iterations.
	 */
	public LinearDelayStrategy(final int minDelay, final int maxDelay, final int iterations) {
		super(iterations);
		this.minDelay = minDelay;
		this.delayStep = (float)(maxDelay - minDelay)/(iterations - 1);
	}

	@Override
	protected int nextInternal(final int iteration) {
		return this.minDelay + (int)(delayStep * (iteration - 1));
	}
}
