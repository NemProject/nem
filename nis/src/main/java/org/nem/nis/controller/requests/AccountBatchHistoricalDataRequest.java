package org.nem.nis.controller.requests;

import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.Deserializer;

import java.util.*;

import static org.nem.nis.controller.requests.HistoricalDataRequest.MAX_DATA_POINTS;

/**
 * Model that contains data for requesting batch historical account data.
 */
public class AccountBatchHistoricalDataRequest {
	private final Collection<SerializableAccountId> accountIds;
	private final HistoricalDataRequest historicalDataRequest;

	/**
	 * Creates a new batch historical account data request object.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountBatchHistoricalDataRequest(final Deserializer deserializer) {
		this.accountIds = deserializer.readObjectArray("accounts", SerializableAccountId::new);
		this.historicalDataRequest = new HistoricalDataRequest(deserializer);
		this.checkConsistency();
	}

	private void checkConsistency() {
		final long range = this.historicalDataRequest.getEndHeight().subtract(this.historicalDataRequest.getStartHeight());
		final long dataPointsPerAccount = range / this.historicalDataRequest.getIncrement();
		if (MAX_DATA_POINTS < dataPointsPerAccount * this.getAccountIds().size()) {
			throw new IllegalArgumentException(
					String.format("only up to %d data points are supported for batch operations", MAX_DATA_POINTS));
		}
	}

	/**
	 * Gets the account ids.
	 *
	 * @return The account ids.
	 */
	public Collection<SerializableAccountId> getAccountIds() {
		return Collections.unmodifiableCollection(this.accountIds);
	}

	/**
	 * Gets the start height.
	 *
	 * @return The start height.
	 */
	public BlockHeight getStartHeight() {
		return this.historicalDataRequest.getStartHeight();
	}

	/**
	 * Gets the end height.
	 *
	 * @return The end height.
	 */
	public BlockHeight getEndHeight() {
		return this.historicalDataRequest.getEndHeight();
	}

	/**
	 * Gets the increment.
	 *
	 * @return The increment.
	 */
	public Long getIncrement() {
		return this.historicalDataRequest.getIncrement();
	}
}
