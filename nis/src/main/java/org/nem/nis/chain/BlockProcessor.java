package org.nem.nis.chain;

import org.nem.core.model.Transaction;

/**
 * An interface for processing a block.
 */
public interface BlockProcessor {

	/**
	 * Processes the block.
	 */
	public void process();

	/**
	 * Processes a transaction.
	 *
	 * @param transaction The transaction.
	 */
	public void process(final Transaction transaction);
}
