package org.nem.nis.controller.requests;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.StringUtils;

/**
 * Builder that is used by Spring to create a BlockHeight from a GET request.
 */
public class BlockHeightBuilder {
	private String blockHeight;

	/**
	 * Sets the block height.
	 *
	 * @param blockHeight The block height.
	 */
	public void setHeight(final String blockHeight) {
		this.blockHeight = blockHeight;
	}

	/**
	 * Creates a BlockHeight.
	 *
	 * @return The block height.
	 */
	public BlockHeight build() {
		return new BlockHeight(StringUtils.isNullOrEmpty(this.blockHeight) ? Long.MAX_VALUE : Long.parseLong(this.blockHeight, 10));
	}
}
