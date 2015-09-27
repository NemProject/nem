package org.nem.nis.test;

/**
 * Common place to have NIS test-related constants.
 */
public class NisTestConstants {

	// TODO 20150921 BR -> J had to change this to get tests passing. Need to find a way to fix it.
	/**
	 * The maximum number of transactions per block (the is larger than the "real" default").
	 */
	public static final int MAX_TRANSACTIONS_PER_BLOCK = 120;

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
}
