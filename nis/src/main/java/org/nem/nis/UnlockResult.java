package org.nem.nis;

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
	 * The account could not be unlocked because it is ineligible for foraging.
	 */
	FAILURE_FORAGING_INELIGIBLE
}