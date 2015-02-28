package org.nem.nis;

/**
 * Hard fork constants.
 */
public class BlockMarkerConstants {

	/**
	 * Beta hard fork due to:
	 * - additional validation of remote account (RemoteNonOperationalValidator is enabled at this fork)
	 */
	public static final long BETA_REMOTE_VALIDATION_FORK = 23552; // 23*1024

	/**
	 * Beta hard fork due to:
	 * - changes in transaction execution (per-transaction instead of per-block)
	 * - additional validation of multisig account (MultisigNonOperationalValidator is reenabled at this fork)
	 * - changes in poi options based on poi analysis
	 */
	public static final long BETA_EXECUTION_CHANGE_FORK = 43000;
}
