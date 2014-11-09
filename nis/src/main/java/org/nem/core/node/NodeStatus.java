package org.nem.core.node;

/**
 * Possible statuses of a NEM node.
 */
public enum NodeStatus {
	/**
	 * The node is active and the last request succeeded.
	 */
	ACTIVE,

	/**
	 * The node is active but the last request failed due to a busy signal.
	 */
	BUSY,

	/**
	 * The node is offline.
	 */
	INACTIVE,

	/**
	 * The node is active but the last request failed due to a server error.
	 */
	FAILURE,

	/**
	 * The node is in an unknown state.
	 */
	UNKNOWN
}