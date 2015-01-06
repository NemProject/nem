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
	 * TODO 20140106 J-G: guess we should change FAILURE_FORAGING_INELIGIBLE too?
	 */
	FAILURE_FORAGING_INELIGIBLE,

	/**
	 * The account could not be unlocked because limit on the server has been hit.
	 */
	FAILURE_SERVER_LIMIT
}