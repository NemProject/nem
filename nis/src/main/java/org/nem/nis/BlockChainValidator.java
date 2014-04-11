package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;

import java.math.BigInteger;
import java.util.Collection;

/**
 * Helper class for validating a block chain.
 */
public class BlockChainValidator {

	private final int maxChainSize;
	private final BlockScorer scorer;
	private final AccountLookup accountLookup;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param scorer The block scorer to use.
	 * @param accountLookup An account lookup that should be used.
	 *
	 * TODO: see if there's a way we can avoid an account lookup here
	 */
	public BlockChainValidator(final BlockScorer scorer, final int maxChainSize, final AccountLookup accountLookup) {
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.accountLookup = accountLookup;
	}

	/**
	 * Determines if blocks is a valid block chain given blocks and parentBlock.
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The block chain.
	 * @return true if the blocks are valid.
	 */
	boolean isValid(Block parentBlock, final Collection<Block> blocks) {
		if (blocks.size() > this.maxChainSize)
			return false;

		long expectedHeight = parentBlock.getHeight() + 1;
		for (final Block block : blocks) {
			if (expectedHeight != block.getHeight() || !block.verify() || !isBlockHit(parentBlock, block))
				return false;

			for (final Transaction transaction : block.getTransactions()) {
				if (!transaction.isValid() || !transaction.verify())
					return false;
			}

			parentBlock = block;
			++expectedHeight;
		}

		return true;
	}

	/**
	 * Computes partial score given blocks and parentBlock.
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The block chain.
	 *
	 * @return "partial score" of blocks.
	 */
	long computePartialScore(Block parentBlock, final Collection<Block> blocks) {
		long peersScore = 0L;

		// used to distinguish first element, to calculate:
		// 2*x_0 + x_1 + x_2 + ...
		boolean isFirst = true;
		for (final Block block : blocks) {
			long score = scorer.calculateBlockScore(
					HashUtils.calculateHash(parentBlock),
					block.getSigner().getKeyPair().getPublicKey()
			);

			peersScore += score;

			if (isFirst) {
				peersScore += score;
				isFirst = false;
			}

			parentBlock = block;
		}

		return peersScore;
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		Account forgerAccount = this.accountLookup.findByAddress(block.getSigner().getAddress());
		final BigInteger hit = this.scorer.calculateHit(parentBlock);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block, forgerAccount);
		return hit.compareTo(target) < 0;
	}
}
