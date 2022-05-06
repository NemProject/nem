package org.nem.core.async;

import java.util.List;

/**
 * An aggregate delay strategy.
 */
public class AggregateDelayStrategy extends AbstractDelayStrategy {

	private final List<AbstractDelayStrategy> strategies;
	private int strategyIndex;

	/**
	 * Creates a new aggregate delay strategy.
	 *
	 * @param strategies The sub strategies.
	 */
	public AggregateDelayStrategy(final List<AbstractDelayStrategy> strategies) {
		this.strategies = strategies;
	}

	@Override
	public boolean shouldStop() {
		for (; this.strategyIndex < this.strategies.size(); ++this.strategyIndex) {
			if (!this.getCurrent().shouldStop()) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected int nextInternal(final int iteration) {
		return this.getCurrent().next();
	}

	private AbstractDelayStrategy getCurrent() {
		return this.strategies.get(this.strategyIndex);
	}
}
