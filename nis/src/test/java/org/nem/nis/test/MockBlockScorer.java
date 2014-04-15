package org.nem.nis.test;

import org.nem.core.model.Account;
import org.nem.core.model.Block;
import org.nem.nis.BlockScorer;

import java.math.BigInteger;
import java.util.*;

/**
 * A mock BlockScorer implementation.
 */
public class MockBlockScorer extends BlockScorer {

	private Account lastCalculateTargetBlockSigner;
	private Set<Block> zeroTargetBlocks = new HashSet<>();

	/**
	 * Configures the scorer to return a ZERO target for the specified block.
	 *
	 * @param block The block.
	 */
	public void setZeroTargetBlock(final Block block) {
		this.zeroTargetBlocks.add(block);
	}

	/**
	 * Gets the last block signer passed to calculateTarget.
	 *
	 * @return The last block signer passed to calculateTarget.
	 */
	public Account getLastCalculateTargetBlockSigner() {

		return this.lastCalculateTargetBlockSigner;
	}

	@Override
	public BigInteger calculateHit(final Block block, final Account blockSigner) {
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger calculateTarget(final Block prevBlock, final Block block, final Account blockSigner) {
		this.lastCalculateTargetBlockSigner = blockSigner;
		return zeroTargetBlocks.contains(block) ? BigInteger.ZERO : BigInteger.TEN;
	}
}