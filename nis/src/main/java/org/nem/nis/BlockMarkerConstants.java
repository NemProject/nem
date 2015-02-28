package org.nem.nis;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Beta hard fork due to additional validation of remote account.
	 * Targeting some day.
	 */
	public static final long BETA_REMOTE_VALIDATION_FORK = 23552; // 23*1024

	/**
	 * Beta hard fork due to changes in transaction execution.
	 * Targeting some day.
	 */
	public static final long BETA_EXECUTION_CHANGE_FORK = 40000;
}
