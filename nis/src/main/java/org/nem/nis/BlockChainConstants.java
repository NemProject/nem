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
	 * Maximum number of blocks send and compare during synchronization
	 */
	public static final int BLOCKS_LIMIT = REWRITE_LIMIT + 40;

	/**
	 * Maximum number of transactions to send during synchronization
	 */
	public static final int TRANSACTIONS_LIMIT = 10000;

	/**
	 * The default number of blocks that are pulled from the database when serving a /chain/blocks-after request.
	 */
	public static final int DEFAULT_NUMBER_OF_BLOCKS_TO_PULL = 100;

	/**
	 * The default maximum of transactions that are allowed to be in the blocks when serving a /chain/blocks-after request.
	 */
	public static final int DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS = 5000;

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
}
