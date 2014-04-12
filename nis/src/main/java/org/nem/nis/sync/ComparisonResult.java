package org.nem.nis.sync;

/**
 * Possible comparison results.
 * TODO: this is probably too fine-grained and can be generalized.
 */
public class ComparisonResult {

	/**
	 * Flag indicating that the remote is evil and should be penalized.
	 */
	public static final int REMOTE_IS_EVIL = 0x80000000;

	/**
	 * The result of the comparison is unknown.
	 */
	public static final int UNKNOWN = 0;

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
	 * The remote node is not in sync with the local node.
	 */
	public static final int REMOTE_IS_NOT_SYNCED = 4;

	/**
	 * The remote node has returned a non-verifiable block.
	 */
	public static final int REMOTE_HAS_NON_VERIFIABLE_BLOCK = REMOTE_IS_EVIL | 1;

	/**
	 * The remote node has returned too many hashes.
	 */
	public static final int REMOTE_RETURNED_TOO_MANY_HASHES = REMOTE_IS_EVIL | 2;

	/**
	 * The remote node has returned invalid hashes.
	 */
	public static final int REMOTE_RETURNED_INVALID_HASHES = REMOTE_IS_EVIL | 3;
}
