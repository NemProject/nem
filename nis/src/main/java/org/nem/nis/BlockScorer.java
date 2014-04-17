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
import java.util.List;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {

	/**
	 * 1_000_000_000 NEMs have force to generate block every minute.
	 */
	public static final long INITIAL_DIFFICULTY = 120000000000L;
	
	/**
	 * Minimum value for difficulty.
	 */
	public static final long MIN_DIFFICULTY = INITIAL_DIFFICULTY / 10L;
	
	/**
	 * Maximum value for difficulty.
	 */
	public static final long Max_DIFFICULTY = INITIAL_DIFFICULTY * 10L;
	
	/**
	 * The target time between two blocks in seconds.
	 */
	public static final long TARGET_TIME_BETWEEN_BLOCKS = 86400L / BlockChain.ESTIMATED_BLOCKS_PER_DAY;

	/**
	 * BigInteger constant 2^64
	 */
    public static final BigInteger TWO_TO_THE_POWER_OF_64 = new BigInteger("18446744073709551616");

	/**
	 * Number of blocks which the calculation of difficulty should include 
	 */
    public static final long NUM_BLOCKS_FOR_AVERAGE_CALCULATION = 60;

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
						 .multiply(TWO_TO_THE_POWER_OF_64)
						 .divide(BigInteger.valueOf(block.getDifficulty()));
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
	 * Calculates the difficulty based the last n blocks.
	 * 
	 * @param historicalBlocks historical blocks, i.e. the last n blocks.
	 *
	 * @return The difficulty for the next block.
	 */
	public long calculateDfficulty(final List<Block> historicalBlocks) {
		if (historicalBlocks.size() < 2) {
			return INITIAL_DIFFICULTY;
		}
		else {
			Block lastBlock = historicalBlocks.get(historicalBlocks.size() - 1);
			Block firstBlock = historicalBlocks.get(0);
			long timeDiff = lastBlock.getTimeStamp().subtract(firstBlock.getTimeStamp());
			final long heightDiff = lastBlock.getHeight() - firstBlock.getHeight();
			long averageDifficulty = 0;
			for (Block block : historicalBlocks) {
				averageDifficulty += block.getDifficulty();
			}
			averageDifficulty /= historicalBlocks.size();
			
			long difficulty = BigInteger.valueOf(averageDifficulty).multiply(BigInteger.valueOf(TARGET_TIME_BETWEEN_BLOCKS))
																   .multiply(BigInteger.valueOf(heightDiff))
																   .divide(BigInteger.valueOf(timeDiff))
																   .longValue();
            if (difficulty < MIN_DIFFICULTY) {
            	difficulty = MIN_DIFFICULTY;
            }
            if (difficulty > Max_DIFFICULTY) {
            	difficulty = Max_DIFFICULTY;
            }
            return difficulty;
		}
	}
}
