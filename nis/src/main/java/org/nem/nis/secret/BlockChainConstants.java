package org.nem.nis.secret;

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
	 * The maximum number of seconds in the future that an entity's timestamp can be
	 * without the entity being rejected.
	 */
	public static final int MAX_ALLOWED_SECONDS_AHEAD_OF_TIME = 10;

	/**
	 * The maximum number of transactions in a block.
	 */
	public static final int MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 60;
}
