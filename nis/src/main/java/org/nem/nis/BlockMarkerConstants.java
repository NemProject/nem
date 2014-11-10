package org.nem.nis;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Beta hard fork due to changing certain sync constants
	 * (e.g. BLOCKS_LIMIT).
	 * <br/>
	 * Targeting Wed, 17 UTC.
	 */
	public static long BETA_HARD_FORK = 24244;

	/**
	 * Beta hard fork due to changing number of transactions inside the block
	 * <br/>
	 * Targeting Wed 12 Nov, 5 UTC.
	 */
	public static long BETA_TX_COUNT_FORK = 33489;
}
