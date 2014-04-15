package org.nem.nis;

import org.nem.core.crypto.Hashes;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.Hash;
import org.nem.core.model.HashUtils;
import org.nem.core.utils.ArrayUtils;
import org.nem.core.utils.ByteUtils;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.Arrays;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {

	/**
	 * 500_000_000 NEMs have force to generate block every minute.
	 */
	public static final long INITIAL_MAGIC_MULTIPLIER = 614891469L;
	
	/**
	 * Minimum value for magic multiplier.
	 */
	public static final long MIN_MAGIC_MULTIPLIER = INITIAL_MAGIC_MULTIPLIER/10L;
	
	/**
	 * Maximum value for magic multiplier.
	 */
	public static final long Max_MAGIC_MULTIPLIER = INITIAL_MAGIC_MULTIPLIER*10L;
	
	/**
	 * The target time between two blocks in seconds.
	 */
	public static final long TARGET_TIME_BETWEEN_BLOCKS = 60;

	/**
	 * Current magic multiplier used in target calculation.
	 */
	public static long magicMultiplier = INITIAL_MAGIC_MULTIPLIER;

	/**
	 * Calculates the hit score for block.
	 *
	 * @param block The block.
	 * @return the hit score.
	 */
	public BigInteger calculateHit(final Block block, final Account blockSigner) {
		byte[] hash = Hashes.sha3(ArrayUtils.concat(blockSigner.getKeyPair().getPublicKey().getRaw(), HashUtils.calculateHash(block).getRaw()));
		return new BigInteger(1, Arrays.copyOfRange(hash, 10, 18));
	}

	/**
	 * Calculates the target score for block given the previous block.
	 *
	 * @param prevBlock The previous block.
	 * @param block The block.
	 * @return The target score.
	 */
	public BigInteger calculateTarget(final Block prevBlock, final Block block) {
		return this.calculateTarget(prevBlock, block, block.getSigner());
	}

	/**
	 * Calculates the target score for block given the previous block using an external block signer account.
	 *
	 * @param prevBlock The previous block.
	 * @param block The block.
	 * @param blockSigner The block signer.
	 * @return The target score.
	 */
	public BigInteger calculateTarget(final Block prevBlock, final Block block, final Account blockSigner) {
		int timeStampDifference = block.getTimeStamp().subtract(prevBlock.getTimeStamp());
		if (timeStampDifference < 0)
			return BigInteger.ZERO;

		long forgerBalance = blockSigner.getBalance().getNumNem();
		return BigInteger.valueOf(timeStampDifference)
				.multiply(BigInteger.valueOf(forgerBalance))
				.multiply(BigInteger.valueOf(magicMultiplier));
	}

	/**
	 * Calculates the block score for block.
	 *
	 * @param parentBlock previous block in the chain.
	 * @param currentBlock currently analyzed block.
	 *
	 * @return The block score.
	 */
	public long calculateBlockScore(final Block parentBlock, final Block currentBlock) {
		int timeDiff = currentBlock.getTimeStamp().subtract(parentBlock.getTimeStamp());
		return calculateBlockScoreImpl(HashUtils.calculateHash(parentBlock), currentBlock.getSigner().getKeyPair().getPublicKey(), timeDiff);
	}

	public long calculateBlockScore(final org.nem.nis.dbmodel.Block parentBlock, final org.nem.nis.dbmodel.Block currentBlock) {
		int timeDiff = (currentBlock.getTimestamp() - parentBlock.getTimestamp());
		return calculateBlockScoreImpl(parentBlock.getBlockHash(), currentBlock.getForger().getPublicKey(), timeDiff);
	}

	/**
	 * @param timeDiff positive time difference between blocks
	 */
	private long calculateBlockScoreImpl(final Hash parentBlockHash, final PublicKey thisBlockSigner, int timeDiff) {
		byte[] hash = Hashes.sha3(ArrayUtils.concat(thisBlockSigner.getRaw(), parentBlockHash.getRaw()));
		return intToUlong(ByteUtils.bytesToInt(Arrays.copyOfRange(hash, 10, 14)));
	}

	private static long intToUlong(int value) {
		//final long fix = Math.abs((long)Integer.MIN_VALUE);
		return Math.abs((long)value);
	}
	
	/**
	 * Calculates the new magic multiplier.
	 */
	public void calculateMagicMultiplier(final Block lastBlock, final Block historicalBlock) {
		if (lastBlock.getHeight() == 1) {
			magicMultiplier = INITIAL_MAGIC_MULTIPLIER;
		}
		else {
			if (lastBlock.getHeight() <= historicalBlock.getHeight()) {
				throw new InvalidParameterException("historicalBlock must have lower height than lastBlock");
			}
			
			final long timeDiff = lastBlock.getTimeStamp().subtract(historicalBlock.getTimeStamp());
			final long heightDiff = lastBlock.getHeight() - historicalBlock.getHeight();
			magicMultiplier = BigInteger.valueOf(magicMultiplier).multiply(BigInteger.valueOf(timeDiff))
																 .divide(BigInteger.valueOf(heightDiff))
																 .divide(BigInteger.valueOf(TARGET_TIME_BETWEEN_BLOCKS))
																 .longValue();
            if (magicMultiplier < MIN_MAGIC_MULTIPLIER) {
            	magicMultiplier = MIN_MAGIC_MULTIPLIER;
            }
            if (magicMultiplier > Max_MAGIC_MULTIPLIER) {
            	magicMultiplier = Max_MAGIC_MULTIPLIER;
            }
		}
	}
}
