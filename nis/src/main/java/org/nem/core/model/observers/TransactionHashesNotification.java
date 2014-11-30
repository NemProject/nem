package org.nem.core.model.observers;

import org.nem.core.model.HashTimeInstantPair;

import java.util.List;

/**
 * A notification that transaction hashes appeared or disappeared from the blockchain.
 */
public class TransactionHashesNotification extends Notification {
	private final List<HashTimeInstantPair> pairs;

	/**
	 * Creates a new transaction hashes notification.
	 *
	 * @param pairs The pairs.
	 */
	public TransactionHashesNotification(final List<HashTimeInstantPair> pairs) {
		super(NotificationType.TransactionHashes);
		this.pairs = pairs;
	}

	/**
	 * Gets the collection of hashes.
	 *
	 * @return The collection of hashes.
	 */
	public List<HashTimeInstantPair> getPairs() {
		return this.pairs;
	}
}
