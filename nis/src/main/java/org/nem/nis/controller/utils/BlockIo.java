package org.nem.nis.controller.utils;

import org.nem.core.model.Block;
import org.nem.core.model.BlockHeight;
import org.nem.core.model.Hash;

public interface BlockIo {

	/**
	 * Requests information about the block at the specified height.
	 *
	 * @param blockHeight The block height.
	 * @return The block at specified height.
	 */
	Block getBlockAt(BlockHeight blockHeight);

	/**
	 * Request information about the block having the specified hash
	 *
	 * @param blockHash The block hash.
	 * @return The block with specified hash.
	 */
	Block getBlock(Hash blockHash);
}
