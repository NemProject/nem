package org.nem.nis.service;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;

public interface BlockIo {

	/**
	 * Requests information about the block at the specified height.
	 *
	 * @param blockHeight The block height.
	 * @return The block at specified height.
	 */
	Block getBlockAt(BlockHeight blockHeight);
}
