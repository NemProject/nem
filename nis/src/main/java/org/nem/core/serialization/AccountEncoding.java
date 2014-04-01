package org.nem.core.serialization;

/**
 * Account encoding modes.
 */
public enum AccountEncoding {
	/**
	 * Encodes the account as an address (the default).
	 */
	ADDRESS,

	/**
	 * Encodes the account as a public key.
	 */
	PUBLIC_KEY
}