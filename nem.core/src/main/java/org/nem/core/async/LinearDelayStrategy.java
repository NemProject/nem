package org.nem.core.async;

/**
 * DelayStrategy that linearly increases a delay from a minimum value
 * to a maximum value.
 */
public class LinearDelayStrategy extends AbstractDelayStrategy {

	private final int minDelay;
	private final float delayStep;

	/**
	 * Creates a new linear delay strategy.
	 *
	 * @param minDelay The minimum delay.
	 * @param maxDelay The maximum delay.
	 * @param iterations The number of iterations.
	 */
	public LinearDelayStrategy(final int minDelay, final int maxDelay, final int iterations) {
		super(iterations);
		this.minDelay = minDelay;
		this.delayStep = (float)(maxDelay - minDelay) / (iterations - 1);
	}

	@Override
	protected int nextInternal(final int iteration) {
		return this.minDelay + (int)(this.delayStep * (iteration - 1));
	}

	/**
	 * Creates a new linear delay strategy with an approximate back-off duration.
	 *
	 * @param minDelay The minimum delay.
	 * @param maxDelay The maximum delay.
	 * @param duration The desired (approximate duration).
	 * @return A new strategy.
	 */
	public static LinearDelayStrategy withDuration(final int minDelay, final int maxDelay, final int duration) {
		if (duration < minDelay + maxDelay) {
			throw new IllegalArgumentException("duration must be at least minDelay + maxDelay");
		}

		final int iterations = 2 * duration / (minDelay + maxDelay);
		return new LinearDelayStrategy(minDelay, maxDelay, iterations);
	}
}
