package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.pox.poi.GroupedHeight;
import org.nem.nis.state.ReadOnlyAccountImportance;

import java.math.BigInteger;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {

	/**
	 * BigInteger constant 2^64
	 */
	public static final BigInteger TWO_TO_THE_POWER_OF_64 = new BigInteger("18446744073709551616");

	/**
	 * BigInteger constant 2^54
	 */
	public static final long TWO_TO_THE_POWER_OF_54 = 18014398509481984L;

	/**
	 * Number of blocks which the calculation of difficulty should include
	 */
	public static final int NUM_BLOCKS_FOR_AVERAGE_CALCULATION = 60;

	/**
	 * Helper constant calculating the logarithm of BigInteger
	 */
	private static final double TWO_TO_THE_POWER_OF_256 = Math.pow(2.0, 256.0);

	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a new block scorer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public BlockScorer(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Calculates the hit score for block.
	 *
	 * @param block The block.
	 * @return the hit score.
	 */
	public BigInteger calculateHit(final Block block) {
		BigInteger val = new BigInteger(1, block.getGenerationHash().getRaw());
		final double tmp = Math.abs(Math.log(val.doubleValue() / TWO_TO_THE_POWER_OF_256));
		val = BigInteger.valueOf((long) (TWO_TO_THE_POWER_OF_54 * tmp));
		return val;
	}

	/**
	 * Calculates the target score for block given the previous block using an external block signer account.
	 *
	 * @param prevBlock The previous block.
	 * @param block The block.
	 * @return The target score.
	 */
	public BigInteger calculateTarget(final Block prevBlock, final Block block) {
		final int timeStampDifference = block.getTimeStamp().subtract(prevBlock.getTimeStamp());
		if (timeStampDifference < 0) {
			return BigInteger.ZERO;
		}

		final long harvesterEffectiveImportance = this.calculateHarvesterEffectiveImportance(block);
		return BigInteger.valueOf(timeStampDifference).multiply(BigInteger.valueOf(harvesterEffectiveImportance))
				.multiply(this.getMultiplierAt(timeStampDifference)).divide(block.getDifficulty().asBigInteger());
	}

	// TODO 20150928 J-B: should add tests for this (1. STABLIZE enabled; 2. non-default generation target time)
	private BigInteger getMultiplierAt(final int timeDiff) {
		final BlockChainConfiguration configuration = NemGlobals.getBlockChainConfiguration();
		final double targetTime = (double) configuration.getBlockGenerationTargetTime();
		final double tmp = configuration.isBlockChainFeatureSupported(BlockChainFeature.STABILIZE_BLOCK_TIMES)
				? Math.min(Math.exp(6.0 * (timeDiff - targetTime) / targetTime), 100.0)
				: 1.0;
		return BigInteger.valueOf((long) (BlockScorer.TWO_TO_THE_POWER_OF_54 * tmp)).shiftLeft(10);
	}

	/**
	 * Calculates harvester effective importance for block.
	 *
	 * @param block The signed, "hit" block.
	 * @return The harvester effective importance.
	 */
	public long calculateHarvesterEffectiveImportance(final Block block) {
		final Amount nemesisAmount = NetworkInfos.getDefault().getNemesisBlockInfo().getAmount();
		final BlockHeight groupedHeight = GroupedHeight.fromHeight(block.getHeight());
		final long multiplier = nemesisAmount.getNumNem();
		final Address signerAddress = block.getSigner().getAddress();
		final ReadOnlyAccountImportance accountImportance = this.accountStateCache
				.findForwardedStateByAddress(signerAddress, block.getHeight()).getImportanceInfo();
		return (long) (accountImportance.getImportance(groupedHeight) * multiplier);
	}

	/**
	 * Calculates the block score for the specified block.
	 *
	 * @param parentBlock The parent block.
	 * @param currentBlock The currently analyzed block.
	 * @return The block score.
	 */
	public long calculateBlockScore(final Block parentBlock, final Block currentBlock) {
		final int timeDiff = currentBlock.getTimeStamp().subtract(parentBlock.getTimeStamp());
		return this.calculateBlockScoreImpl(timeDiff, currentBlock.getDifficulty().getRaw());
	}

	private long calculateBlockScoreImpl(final int timeDiff, final long difficulty) {
		return difficulty - timeDiff;
	}

	/**
	 * Returns the default block difficulty scorer.
	 *
	 * @return The block difficulty scorer.
	 */
	public BlockDifficultyScorer getDifficultyScorer() {
		return new BlockDifficultyScorer();
	}
}
