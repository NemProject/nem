package org.nem.core.async;

/**
 * DelayStrategy that produces a uniform delay.
 */
public class UniformDelayStrategy extends AbstractDelayStrategy {

	private final int delay;

	/**
	 * Creates a new infinite uniform delay strategy.
	 *
	 * @param delay The delay interval.
	 */
	public UniformDelayStrategy(final int delay) {
		this.delay = delay;
	}

	/**
	 * Creates a new finite uniform delay strategy.
	 *
	 * @param delay The delay interval.
	 * @param maxDelays The maximum number of delays.
	 */
	public UniformDelayStrategy(final int delay, final int maxDelays) {
		super(maxDelays);
		this.delay = delay;
	}

	@Override
	protected int nextInternal(final int iteration) {
		return this.delay;
	}
}
