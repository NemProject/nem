package org.nem.nis.controller.requests;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.MustBe;

/**
 * Model that contains data for requesting historical account data.
 */
public class HistoricalDataRequest {
	public static final long MAX_DATA_POINTS = 10000;

	private final BlockHeight startHeight;
	private final BlockHeight endHeight;
	private final Long increment;

	/**
	 * Creates a new historical data request object.
	 *
	 * @param startHeight The start height.
	 * @param endHeight The end height.
	 * @param increment The increment by which to increase the height.
	 */
	public HistoricalDataRequest(final BlockHeight startHeight, final BlockHeight endHeight, final Long increment) {
		this.startHeight = startHeight;
		this.endHeight = endHeight;
		this.increment = increment;
		this.checkConsistency();
	}

	/**
	 * Creates a new historical data request object.
	 *
	 * @param deserializer The deserializer.
	 */
	public HistoricalDataRequest(final Deserializer deserializer) {
		this.startHeight = BlockHeight.readFrom(deserializer, "startHeight");
		this.endHeight = BlockHeight.readFrom(deserializer, "endHeight");
		this.increment = deserializer.readLong("incrementBy");
		this.checkConsistency();
	}

	private void checkConsistency() {
		MustBe.notNull(this.startHeight, "startHeight");
		MustBe.notNull(this.endHeight, "endHeight");
		MustBe.notNull(this.increment, "increment");

		final long range = this.endHeight.subtract(this.startHeight);
		if (0 > range) {
			throw new IllegalArgumentException("start and end height are out of valid range");
		}

		if (0 >= this.increment) {
			throw new IllegalArgumentException("increment must be a positive integer");
		}

		if (MAX_DATA_POINTS < range / this.increment) {
			throw new IllegalArgumentException(String.format("only up to %d data points are supported", MAX_DATA_POINTS));
		}
	}

	/**
	 * Gets the start height.
	 *
	 * @return The start height.
	 */
	public BlockHeight getStartHeight() {
		return this.startHeight;
	}

	/**
	 * Gets the end height.
	 *
	 * @return The end height.
	 */
	public BlockHeight getEndHeight() {
		return this.endHeight;
	}

	/**
	 * Gets the increment.
	 *
	 * @return The increment.
	 */
	public Long getIncrement() {
		return this.increment;
	}
}
