package org.nem.nis;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.core.model.Hash;
import org.nem.core.model.HashUtils;
import org.nem.core.utils.ByteUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {

	/**
	 * 500_000_000 NEMs have force to generate block every minute.
	 */
	public static final long MAGIC_MULTIPLIER = 614891469L;

	/**
	 * Calculates the hit score for block.
	 *
	 * @param block The block.
	 * @return the hit score.
	 */
	BigInteger calculateHit(final Block block) {
		return new BigInteger(1, Arrays.copyOfRange(block.getSignature().getBytes(), 2, 10));
	}

	/**
	 * Calculates the target score for block given the previous block.
	 *
	 * @param prevBlock The previous block.
	 * @param block The block.
	 * @return The target score.
	 */
	BigInteger calculateTarget(final Block prevBlock, final Block block) {
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
	BigInteger calculateTarget(final Block prevBlock, final Block block, final Account blockSigner) {
		int timeStampDifference = block.getTimeStamp().subtract(prevBlock.getTimeStamp());
		if (timeStampDifference < 0)
			return BigInteger.ZERO;

		long forgerBalance = blockSigner.getBalance().getNumNem();
		return BigInteger.valueOf(timeStampDifference)
				.multiply(BigInteger.valueOf(forgerBalance))
				.multiply(BigInteger.valueOf(MAGIC_MULTIPLIER));
	}

	/**
	 * Calculates the block score for block.
	 *
	 * @param parentBlockHash previous (parent block hash)
	 * @param thisBlockSigner signer of block
	 *
	 * @return The block score.
	 *
	 * TODO: this api could take dbBlock, but this way we have one API for
	 * both blocks and dbblocks.
	 */
	long calculateBlockScore(final Hash parentBlockHash, final PublicKey thisBlockSigner) {
		long r1 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(thisBlockSigner.getRaw(), 10, 14)));
		long r2 = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(parentBlockHash.getRaw(), 10, 14)));

		return r1 + r2;
	}
}
