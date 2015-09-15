package org.nem.core.model;

/**
 * Common place to have BlockChain-related constants accessible from all modules.
 */
public class BlockChainConstants {

	//region ESTIMATED_BLOCKS

	/**
	 * Estimated number of blocks that NEM network will produce during single day
	 */
	public static final int ESTIMATED_BLOCKS_PER_DAY = 1440;

	/**
	 * Estimated number of blocks that NEM network will produce during single month
	 */
	public static final int ESTIMATED_BLOCKS_PER_MONTH = ESTIMATED_BLOCKS_PER_DAY * 30;

	/**
	 * Estimated number of blocks that NEM network will produce during single year
	 */
	public static final int ESTIMATED_BLOCKS_PER_YEAR = ESTIMATED_BLOCKS_PER_DAY * 365;

	//endregion

	/**
	 * Number of blocks that network is allowed to rewrite during fork
	 */
	public static final int REWRITE_LIMIT = (ESTIMATED_BLOCKS_PER_DAY / 4);

	/**
	 * Number of blocks required for activating remote harvesting
	 */
	public static final int REMOTE_HARVESTING_DELAY = REWRITE_LIMIT;

	/**
	 * Maximum number of blocks to send and compare during synchronization.
	 */
	public static final int BLOCKS_LIMIT = REWRITE_LIMIT + 40;

	/**
	 * Maximum number of transactions to send during synchronization.
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
	 * The maximum number of transactions in a block at the specified height.
	 */
	public static final int DEFAULT_MAX_ALLOWED_TRANSACTIONS_PER_BLOCK = 120;

	/**
	 * The maximum number of cosignatories that a multisig account can have.
	 */
	public static final int MAX_ALLOWED_COSIGNATORIES_PER_ACCOUNT = 32;

	/**
	 * The maximum number of mosaics allowed in a transfer transaction.
	 */
	public static final int MAX_ALLOWED_MOSAICS_PER_TRANSFER = 10;

	/**
	 * The maximum age (in blocks) of outlinks to use in POI calculations.
	 */
	public static final int OUTLINK_HISTORY = 30 * ESTIMATED_BLOCKS_PER_DAY;
}
