package org.nem.core.model.observers;

import org.nem.core.model.HashMetaDataPair;

import java.util.List;

/**
 * A notification that transaction hashes appeared or disappeared from the blockchain.
 */
public class TransactionHashesNotification extends Notification {
	private final List<HashMetaDataPair> pairs;

	/**
	 * Creates a new transaction hashes notification.
	 *
	 * @param pairs The pairs.
	 */
	public TransactionHashesNotification(final List<HashMetaDataPair> pairs) {
		super(NotificationType.TransactionHashes);
		this.pairs = pairs;
	}

	/**
	 * Gets the collection of hashes.
	 *
	 * @return The collection of hashes.
	 */
	public List<HashMetaDataPair> getPairs() {
		return this.pairs;
	}
}
