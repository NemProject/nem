package org.nem.nis.harvesting;

/**
 * Possible unlock results.
 */
public enum UnlockResult {
	/**
	 * The account was successfully unlocked (it might have already been unlocked).
	 */
	SUCCESS,

	/**
	 * The account could not be unlocked because it is unknown.
	 */
	FAILURE_UNKNOWN_ACCOUNT,

	/**
	 * The account could not be unlocked because it is ineligible for harvesting.
	 */
	FAILURE_HARVESTING_INELIGIBLE,

	/**
	 * The account could not be unlocked because it is blocked from harvesting.
	 */
	FAILURE_HARVESTING_BLOCKED,

	/**
	 * The account could not be unlocked because limit on the server has been hit.
	 */
	FAILURE_SERVER_LIMIT
}
