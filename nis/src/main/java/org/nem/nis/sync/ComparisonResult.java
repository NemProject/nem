package org.nem.nis.sync;

/**
 * Possible comparison results.
 *
 */
public class ComparisonResult {

	/**
	 * Possible comparison end states.
	 * TODO: this is probably too fine-grained and can be generalized.
	 */
	public class Code {
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

	private final int code;
	private final long commonBlockHeight;
	private final boolean areChainsConsistent;

	/**
	 * Creates a new comparison result
	 *
	 * @param code The result code.
	 * @param commonBlockHeight The height of the last common block between two chains.
	 * @param areChainsConsistent true if the two chains are consistent.
	 */
	public ComparisonResult(int code, long commonBlockHeight, boolean areChainsConsistent) {
		this.code = code;
		this.commonBlockHeight = commonBlockHeight;
		this.areChainsConsistent = areChainsConsistent;
	}

	/**
	 * Gets the result code.
	 *
	 * @return The result code.
	 */
	public int getCode() { return this.code; }

	/**
	 * Gets the common block height (only supported when code is REMOTE_IS_NOT_SYNCED).
	 *
	 * @return The common block height.
	 */
	public long getCommonBlockHeight() {
		if (Code.REMOTE_IS_NOT_SYNCED != this.code)
			throw new UnsupportedOperationException("unsupported when code is not REMOTE_IS_NOT_SYNCED");

		return this.commonBlockHeight;
	}

	/**
	 * Gets a value indicating whether or not the chains are consistent (only supported when
	 * code is REMOTE_IS_NOT_SYNCED).
	 *
	 * @return true if the chains are consistent.
	 */
	public boolean areChainsConsistent() {
		if (Code.REMOTE_IS_NOT_SYNCED != this.code)
			throw new UnsupportedOperationException("unsupported when code is not REMOTE_IS_NOT_SYNCED");

		return this.areChainsConsistent;
	}
}
