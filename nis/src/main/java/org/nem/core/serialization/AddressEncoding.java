package org.nem.core.serialization;

/**
 * Address encoding modes.
 */
public enum AddressEncoding {
	/**
	 * Encodes the address as a compressed string (the default).
	 */
	COMPRESSED,

	/**
	 * Encodes the address as a public key.
	 */
	PUBLIC_KEY
}