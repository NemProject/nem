package org.nem.core.model.observers;

import org.nem.core.crypto.Hash;
import org.nem.core.time.TimeInstant;

import java.util.List;

/**
 * A notification that transaction hashes appeared or disappeared from the blockchain.
 */
public class TransactionHashesNotification extends Notification {
	private final List<Hash> hashes;
	private final List<TimeInstant> timeStamps;

	/**
	 * Creates a new transaction hashes notification.
	 *
	 * @param hashes The transaction hashes.
	 * @param timeStamps The transaction time stamps.
	 */
	public TransactionHashesNotification(final List<Hash> hashes, final List<TimeInstant> timeStamps) {
		super(NotificationType.TransactionHashes);
		this.hashes = hashes;
		this.timeStamps = timeStamps;
	}

	/**
	 * Gets the collection of hashes.
	 *
	 * @return The collection of hashes.
	 */
	public List<Hash> getHashes() {
		return this.hashes;
	}

	/**
	 * Gets the collection of timestamps.
	 *
	 * @return The collection of timestamps.
	 */
	public List<TimeInstant> getTimeStamps() {
		return this.timeStamps;
	}
}
