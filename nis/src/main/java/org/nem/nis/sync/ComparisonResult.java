package org.nem.nis.sync;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.peer.NodeInteractionResult;

/**
 * Possible comparison results.
 */
public class ComparisonResult {

	/**
	 * Special code flag indicating that the remote is evil and should be penalized.
	 */
	private static final int REMOTE_IS_EVIL = 0x80000000;

	/**
	 * Possible comparison end states.
	 */
	public enum Code {
		/**
		 * The result of the comparison is unknown.
		 */
		UNKNOWN(0),

		/**
		 * The remote node has no blocks.
		 */
		REMOTE_HAS_NO_BLOCKS(1),

		/**
		 * The remote node is synchronized with the local node.
		 */
		REMOTE_IS_SYNCED(2),

		/**
		 * The remote node is too far behind the local node.
		 */
		REMOTE_IS_TOO_FAR_BEHIND(3),

		/**
		 * The remote node is not in sync with the local node.
		 */
		REMOTE_IS_NOT_SYNCED(4),

		/**
		 * The remote node is reported an equal chain score compared the local node.
		 */
		REMOTE_REPORTED_EQUAL_CHAIN_SCORE(5),

		/**
		 * The remote node is reported a lower chain score than the local node.
		 */
		REMOTE_REPORTED_LOWER_CHAIN_SCORE(6),

		/**
		 * The remote node has returned a non-verifiable block.
		 */
		REMOTE_HAS_NON_VERIFIABLE_BLOCK(REMOTE_IS_EVIL | 1),

		/**
		 * The remote node has returned too many hashes.
		 */
		REMOTE_RETURNED_TOO_MANY_HASHES(REMOTE_IS_EVIL | 2),

		/**
		 * The remote node has returned invalid hashes.
		 */
		REMOTE_RETURNED_INVALID_HASHES(REMOTE_IS_EVIL | 3),

		/**
		 * The remote node lied about having a higher chain score.
		 */
		REMOTE_LIED_ABOUT_CHAIN_SCORE(REMOTE_IS_EVIL | 4);

		private final int value;

		Code(final int value) {
			this.value = value;
		}

		/**
		 * Gets a value indicating whether or not this result indicates an error.
		 *
		 * @return true if this result indicates an error.
		 */
		public boolean isEvil() {
			return 0 != (REMOTE_IS_EVIL & this.value);
		}

		/**
		 * Gets the underlying integer representation of the result.
		 *
		 * @return The underlying value.
		 */
		public int getValue() {
			return this.value;
		}
	}

	private final Code code;
	private final long commonBlockHeight;
	private final BlockHeight remoteHeight;
	private final boolean areChainsConsistent;

	/**
	 * Creates a new comparison result
	 *
	 * @param code The result code.
	 * @param commonBlockHeight The height of the last common block between two chains.
	 * @param areChainsConsistent true if the two chains are consistent.
	 * @param remoteHeight Height of remote chain (can be null).
	 */
	public ComparisonResult(final Code code, final long commonBlockHeight, final boolean areChainsConsistent,
			final BlockHeight remoteHeight) {
		this.code = code;
		this.commonBlockHeight = commonBlockHeight;
		this.areChainsConsistent = areChainsConsistent;
		this.remoteHeight = remoteHeight;
	}

	/**
	 * Gets the result code.
	 *
	 * @return The result code.
	 */
	public Code getCode() {
		return this.code;
	}

	/**
	 * Gets the common block height (only supported when code is REMOTE_IS_NOT_SYNCED).
	 *
	 * @return The common block height.
	 */
	public long getCommonBlockHeight() {
		if (Code.REMOTE_IS_NOT_SYNCED != this.code) {
			throw new UnsupportedOperationException("unsupported when code is not REMOTE_IS_NOT_SYNCED");
		}

		return this.commonBlockHeight;
	}

	/**
	 * Gets full height of remote chain. (only supported when code is not REMOTE_HAS_NO_BLOCKS).
	 *
	 * @return The height of remote chain.
	 */
	public BlockHeight getRemoteHeight() {
		if (Code.REMOTE_HAS_NO_BLOCKS == this.code) {
			throw new UnsupportedOperationException("unsupported when code is not REMOTE_HAS_NO_BLOCKS");
		}

		return this.remoteHeight;
	}

	/**
	 * Gets a value indicating whether or not the chains are consistent (only supported when code is REMOTE_IS_NOT_SYNCED).
	 *
	 * @return true if the chains are consistent.
	 */
	public boolean areChainsConsistent() {
		if (Code.REMOTE_IS_NOT_SYNCED != this.code) {
			throw new UnsupportedOperationException("unsupported when code is not REMOTE_IS_NOT_SYNCED");
		}

		return this.areChainsConsistent;
	}

	/**
	 * Creates a new NodeInteractionResult from a comparison result code.
	 *
	 * @return The NodeInteractionResult.
	 */
	public NodeInteractionResult toNodeInteractionResult() {
		switch (this.code) {
			case REMOTE_IS_SYNCED:
			case REMOTE_REPORTED_EQUAL_CHAIN_SCORE:
			case REMOTE_REPORTED_LOWER_CHAIN_SCORE:
				return NodeInteractionResult.NEUTRAL;
			default :
				break;
		}

		return NodeInteractionResult.FAILURE;
	}
}
