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
	 * Targeting Thursday 13 Nov, 6am UTC.
	 */
	public static long BETA_TX_COUNT_FORK = 35000;
}
