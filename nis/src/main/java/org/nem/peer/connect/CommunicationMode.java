package org.nem.peer.connect;

/**
 * Possible communication modes.
 */
public enum CommunicationMode {

	/**
	 * All communication should use JSON payloads.
	 */
	JSON,

	/**
	 * All communication should use binary payloads.
	 */
	BINARY
}
