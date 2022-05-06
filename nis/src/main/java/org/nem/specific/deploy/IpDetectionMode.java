package org.nem.specific.deploy;

/**
 * Possible IP detection modes.
 */
public enum IpDetectionMode {

	/**
	 * IP detection is automatic and is required for a node to boot.
	 */
	AutoRequired,

	/**
	 * IP detection is automatic but is not required for a node to boot.
	 */
	AutoOptional,

	/**
	 * Automatic IP detection is disabled.
	 */
	Disabled
}
