package org.nem.nis.secret;

import org.nem.core.model.primitive.Amount;

/**
 * Common place to have BlockChain-related constants accessible
 * from both org.nem.nis and org.nem.core packages
 */
public class BlockChainConstants {

	/**
	 * Estimated number of blocks, that NEM network will produce during single day
	 */
	public static final int ESTIMATED_BLOCKS_PER_DAY = 1440;

	/**
	 * Number of blocks that network is allowed to rewrite during fork
	 */
	public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 2);

	/**
	 * Number of blocks send and compared during synchronization
	 */
	public static final int BLOCKS_LIMIT = ESTIMATED_BLOCKS_PER_DAY;

	/**
	 * Minimal balance required to forage.
	 */
	public static final Amount MIN_HARVESTING_BALANCE = Amount.fromNem(1000);
}
