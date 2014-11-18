package org.nem.nis;

/**
 * Hard fork constants.
 * TODO 20141110: this needs to be cleared before official release!
 */
public class BlockMarkerConstants {

	/**
	 * Beta hard fork due to changing certain sync constants (e.g. BLOCKS_LIMIT).
	 * <br/>
	 * Targeting Wed, 17 UTC.
	 */
	public static long BETA_HARD_FORK = 24244;

	/**
	 * Beta hard fork due to changing the maximum number of transactions inside a block.
	 * <br/>
	 * Targeting Friday 13 Nov, 8am UTC.
	 */
	public static long BETA_TX_COUNT_FORK = 36500;

	/**
	 * Beta hard fork due to adding additional importance transfer validation.
	 */
	public static long BETA_IT_VALIDATION_FORK = 44974;
}
