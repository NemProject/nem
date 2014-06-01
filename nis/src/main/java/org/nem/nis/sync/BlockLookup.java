package org.nem.nis.sync;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;

/**
 * An interface for looking up blocks.
 */
public interface BlockLookup {

	/**
	 * Requests information about the last block in the chain.
	 *
	 * @return The last block.
	 */
	public Block getLastBlock();

	/**
	 * Requests information about the block at the specified height.
	 *
	 * @param height The block height.
	 * @return The block at the specified height
	 */
	public Block getBlockAt(final BlockHeight height);

	/**
	 * Requests information about the hashes of all blocks in the chain after the specified height.
	 *
	 * @param height The block height.
	 * @return The hashes of all blocks in the chain after the specified height.
	 */
	public HashChain getHashesFrom(final BlockHeight height);
}
