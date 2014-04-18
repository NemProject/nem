package org.nem.core.model;

import org.nem.core.time.*;

/**
 * Factory class that creates time-synced entities.
 */
public class EntityFactory {

	private final TimeProvider timeProvider;

	/**
	 * Creates an EntityFactory around the specified time provider.
	 *
	 * @param timeProvider The time provider to use.
	 */
	public EntityFactory(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Creates a new time-synced block.
	 *
	 * @param forger        The forger account.
	 * @param prevBlockHash The hash of the previous block.
	 * @param height        The block height.
	 *
	 * @return The new Block.
	 */
	public Block createBlock(final Account forger, final Hash prevBlockHash, final BlockHeight height) {
		return new Block(forger, prevBlockHash, this.timeProvider.getCurrentTime(), height);
	}

	/**
	 * Creates a new time-synced transfer transaction.
	 *
	 * @param sender    The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount    The transaction amount.
	 *
	 * @return The new TransferTransaction.
	 */
	public TransferTransaction createTransfer(final Account sender, final Account recipient, final Amount amount, final Message message) {
		return new TransferTransaction(this.timeProvider.getCurrentTime(), sender, recipient, amount, message);
	}
}
