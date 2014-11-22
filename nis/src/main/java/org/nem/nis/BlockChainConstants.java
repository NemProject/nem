package org.nem.nis;

import org.nem.core.model.primitive.BlockHeight;

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
	public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 4);

	/**
	 * Number of blocks send and compared during synchronization
	 */
	public static final int BLOCKS_LIMIT = REWRITE_LIMIT + 40;

	/**
	 * The maximum number of seconds in the future that an entity's timestamp can be
	 * without the entity being rejected.
	 */
	public static final int MAX_ALLOWED_SECONDS_AHEAD_OF_TIME = 10;

	/**
	 * The maximum number of transactions in a block.
	 */
	public static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 120;

	/**
	 * The maximum number of transactions in a block at the specified height.
	 *
	 * @param height The height.
	 */
	public static int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK(final BlockHeight height) {
		if (height.getRaw() > BlockMarkerConstants.BETA_TX_COUNT_FORK) {
			return MAX_ALLOWED_TRANSACTIONS_PER_BLOCK;
		} else if (height.getRaw() > BlockMarkerConstants.BETA_HARD_FORK) {
			return 60;
		} else {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * The maximum age (in blocks) of outlinks to use in POI calculations.
	 */
	public static final int OUTLINK_HISTORY = 30 * ESTIMATED_BLOCKS_PER_DAY;
}
