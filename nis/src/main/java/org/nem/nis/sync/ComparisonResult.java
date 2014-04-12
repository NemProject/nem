package org.nem.nis.sync;

public class ComparisonResult {

	/**
	 * Flag indicating that the remote is evil and should be penalized.
	 */
	public static final int REMOTE_IS_EVIL = 0x80000000;

	/**
	 * The remote node has not blocks.
	 */
	public static final int REMOTE_HAS_NO_BLOCKS = 1;

	/**
	 * The remote node is synchronized with the local node.
	 */
	public static final int REMOTE_IS_SYNCED = 2;

	/**
	 * The remote node is behind the local node.
	 */
	public static final int REMOTE_IS_BEHIND = 3;

	/**
	 * The remote node is too far behind the local node.
	 */
	public static final int REMOTE_IS_TOO_FAR_BEHIND = 3;

	/**
	 * The remote node is ahead of the local node.
	 */
	public static final int REMOTE_IS_AHEAD = 4;

	/**
	 * The remote node has returned a non-verifiable block.
	 */
	public static final int REMOTE_HAS_NON_VERIFIABLE_BLOCK = REMOTE_IS_EVIL | 1;
}
