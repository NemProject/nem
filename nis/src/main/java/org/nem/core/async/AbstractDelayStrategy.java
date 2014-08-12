package org.nem.core.async;

/**
 * An abstract strategy for providing delays.
 */
public abstract class AbstractDelayStrategy {

	private final Integer maxDelays;
	private int numDelays;

	/**
	 * Creates a new abstract delay strategy.
	 */
	protected AbstractDelayStrategy() {
		this.maxDelays = null;
	}

	/**
	 * Creates a new finite abstract delay strategy.
	 *
	 * @param maxDelays The maximum number of times next is allowed to be called.
	 */
	protected AbstractDelayStrategy(final int maxDelays) {
		this.maxDelays = maxDelays;
	}

	/**
	 * Gets a value indicating whether or not all delays are exhausted
	 * (and the recurring operation should stop).
	 *
	 * @return true if the recurring operation should be stopped.
	 */
	public boolean shouldStop() {
		return null != this.maxDelays && this.maxDelays <= this.numDelays;
	}

	/**
	 * Gets the next delay (in milliseconds).
	 *
	 * @return The next delay.
	 */
	public final int next() {
		if (this.shouldStop()) {
			throw new IllegalStateException("the delay strategy is exhausted");
		}

		return this.nextInternal(++this.numDelays);
	}

	/**
	 * Gets the next delay (in milliseconds).
	 *
	 * @param iteration The 1-based call count.
	 * @return The next delay.
	 */
	protected abstract int nextInternal(final int iteration);
}
