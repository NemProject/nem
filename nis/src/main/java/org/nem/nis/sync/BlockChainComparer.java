package org.nem.nis.sync;

import org.nem.core.model.Block;
import org.nem.core.model.HashChain;
import org.nem.core.model.HashUtils;
import org.nem.nis.BlockScorer;

import java.security.InvalidParameterException;

/**
 * Helper class for comparing two block chains.
 */
public class BlockChainComparer {

	private final ComparisonContext context;

	/**
	 * Creates a block chain comparer that can compare block chains.
	 *
	 * @param context The comparison context.
	 */
	public BlockChainComparer(final ComparisonContext context) {
		this.context = context;
	}

	/**
	 * Compares a local block chain and a remote block chain.
	 *
	 * @param localLookup The local block chain.
	 * @param remoteLookup The remote block chain.
	 * @return The comparison result.
	 */
	public ComparisonResult compare(final BlockLookup localLookup, final BlockLookup remoteLookup) {
		final Impl impl = new Impl(this.context, localLookup, remoteLookup);
		return impl.compare();
	}

	private static boolean areBlocksEqual(final Block lhs, final Block rhs) {
		// TODO: move to Block.equals
		return lhs.getHeight() == rhs.getHeight()
				&& HashUtils.calculateHash(lhs).equals(HashUtils.calculateHash(rhs))
				&& lhs.getSignature().equals(rhs.getSignature());
	}

	private static class Impl {

		private final ComparisonContext context;
		private final BlockLookup localLookup;
		private final BlockLookup remoteLookup;

		private final Block localLastBlock;
		private final Block remoteLastBlock;

		private long commonBlockIndex;
		private boolean areChainsConsistent;

		public Impl(final ComparisonContext context, final BlockLookup localLookup, final BlockLookup remoteLookup) {
			this.context = context;
			this.localLookup = localLookup;
			this.remoteLookup = remoteLookup;

			this.localLastBlock = this.localLookup.getLastBlock();
			if (null == this.localLastBlock)
				throw new InvalidParameterException("Local does not have any blocks");

			this.remoteLastBlock = this.remoteLookup.getLastBlock();
		}

		public ComparisonResult compare() {
			int code = this.compareLastBlock();
			if (ComparisonResult.Code.UNKNOWN == code)
				code = this.compareHashes();


			if (! this.areChainsConsistent) {
				// not to waste our time, first try to get first block and verify it
				// this is just to save time
				final Block differBlock = this.remoteLookup.getBlockAt(this.commonBlockIndex + 1);

				if (! differBlock.verify()) {
					code = ComparisonResult.Code.REMOTE_HAS_NON_VERIFIABLE_BLOCK;
				}
			}

			return new ComparisonResult(code, this.commonBlockIndex, this.areChainsConsistent);
		}

		private boolean isRemoteTooFarBehind() {
			final long heightDifference = localLastBlock.getHeight() - remoteLastBlock.getHeight();
			return heightDifference > this.context.getMaxNumBlocksToRewrite();
		}

		private int compareHashes() {
			final long startingBlockHeight = Math.max(1, this.localLastBlock.getHeight() - this.context.getMaxNumBlocksToRewrite());
			final HashChain remoteHashes = this.remoteLookup.getHashesFrom(startingBlockHeight);
			if (remoteHashes.size() > this.context.getMaxNumBlocksToAnalyze()) // TODO: not sure if we should just used getMaxNumBlocksToRewrite too
				return ComparisonResult.Code.REMOTE_RETURNED_TOO_MANY_HASHES;

			final HashChain localHashes = this.localLookup.getHashesFrom(startingBlockHeight);
			int firstDifferenceIndex = localHashes.findFirstDifferent(remoteHashes);
			if (0 == firstDifferenceIndex) {
				// at least first compared block should be the same, if not, the remote is a liar or on a fork
				return ComparisonResult.Code.REMOTE_RETURNED_INVALID_HASHES;
			}

			if (remoteHashes.size() == firstDifferenceIndex) {
				// nothing to do, we have all of peers blocks
				return ComparisonResult.Code.REMOTE_IS_SYNCED;
			}

			this.commonBlockIndex = startingBlockHeight + firstDifferenceIndex - 1;
			this.areChainsConsistent = firstDifferenceIndex == localHashes.size();
			return ComparisonResult.Code.REMOTE_IS_NOT_SYNCED;
		}

		private int compareLastBlock() {
			if (null == this.remoteLastBlock)
				return ComparisonResult.Code.REMOTE_HAS_NO_BLOCKS;

			if (!remoteLastBlock.verify())
				return ComparisonResult.Code.REMOTE_HAS_NON_VERIFIABLE_BLOCK;

			if (areBlocksEqual(localLastBlock, remoteLastBlock))
				return ComparisonResult.Code.REMOTE_IS_SYNCED;

			if (this.isRemoteTooFarBehind())
				return ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND;

			return ComparisonResult.Code.UNKNOWN;
		}
	}
}
