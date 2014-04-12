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
	public int compare(final BlockLookup localLookup, final BlockLookup remoteLookup) {

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

		public Impl(final ComparisonContext context, final BlockLookup localLookup, final BlockLookup remoteLookup) {
			this.context = context;
			this.localLookup = localLookup;
			this.remoteLookup = remoteLookup;

			this.localLastBlock = this.localLookup.getLastBlock();
			if (null == this.localLastBlock)
				throw new InvalidParameterException("Local does not have any blocks");

			this.remoteLastBlock = this.remoteLookup.getLastBlock();
		}

		public int compare() {
			if (null == this.remoteLastBlock)
				return ComparisonResult.REMOTE_HAS_NO_BLOCKS;

			if (!remoteLastBlock.verify())
				return ComparisonResult.REMOTE_HAS_NON_VERIFIABLE_BLOCK;

			if (areBlocksEqual(localLastBlock, remoteLastBlock))
				return ComparisonResult.REMOTE_IS_SYNCED;

			// TODO: resume tests here

			final long heightDifference = localLastBlock.getHeight() - remoteLastBlock.getHeight();
			if (heightDifference <= -this.context.getMaxNumBlocksToRewrite())
				return ComparisonResult.REMOTE_IS_TOO_FAR_BEHIND;

			final long startingPoint = Math.max(1, localLastBlock.getHeight() - this.context.getMaxNumBlocksToRewrite());
			final HashChain remoteHashes = this.remoteLookup.getHashesFrom(startingPoint);
			if (remoteHashes.size() > this.context.getMaxNumBlocksToAnalyze())
				return ComparisonResult.REMOTE_IS_EVIL; // TODO: too many hashes

			final HashChain localHashes = this.localLookup.getHashesFrom(startingPoint);
			int firstDifferenceIndex = localHashes.findFirstDifferent(remoteHashes);
			if (0 == firstDifferenceIndex) {
				// at least first compared block should be the same, if not, the remote is a liar or on a fork
				return ComparisonResult.REMOTE_IS_EVIL; // TODO: no common block
			}

			if (remoteHashes.size() == firstDifferenceIndex) {
				// nothing to do, we have all of peers blocks
				return ComparisonResult.REMOTE_IS_SYNCED;
			}

			SynchronizeContext context = new SynchronizeContext(startingPoint + firstDifferenceIndex - 1,  localHashes.size() > firstDifferenceIndex);

			final long commonBlockHeight = context.commonBlockHeight;
			// not to waste our time, first try to get first block that differs
			final Block differBlock = this.remoteLookup.getBlockAt(commonBlockHeight + 1);
			if (context.hasOwnChain) {
				if (! this.sychronizeCompareAt(differBlock, commonBlockHeight)) {
					return ComparisonResult.REMOTE_HAS_NO_BLOCKS; // TODO: return something different
				}
			}

			return ComparisonResult.REMOTE_IS_BEHIND;
		}

		private boolean sychronizeCompareAt(Block peerBlock, long commonHeight) {
			if (!peerBlock.verify()) {
//			penalize(node);
				return false;
			}

			Block commonBlock = this.localLookup.getBlockAt(commonHeight);

			Block ourBlock = this.localLookup.getBlockAt(commonHeight + 1);

			if (areBlocksEqual(peerBlock, ourBlock)) {
				return false;
			}

			final BlockScorer scorer = this.context.getBlockScorer();
			long peerScore = scorer.calculateBlockScore(HashUtils.calculateHash(commonBlock), peerBlock.getSigner().getKeyPair().getPublicKey());
			long ourScore = scorer.calculateBlockScore(HashUtils.calculateHash(commonBlock), ourBlock.getSigner().getKeyPair().getPublicKey());
			return peerScore < ourScore;
		}
	}
}
