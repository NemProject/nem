package org.nem.nis;

import org.nem.core.model.primitive.BlockHeight;

/**
 * DTO for the fee fork.
 */
public class FeeFork {
	public final BlockHeight firstHeight;
	public final BlockHeight secondHeight;

	/**
	 * Creates a fee fork dto.
	 *
	 * @param firstHeight The first fee fork height.
	 * @param secondHeight The second fee fork height.
	 */
	public FeeFork(final BlockHeight firstHeight, final BlockHeight secondHeight) {
		this.firstHeight = firstHeight;
		this.secondHeight = secondHeight;
	}

	/**
	 * Gets the first fee fork height.
	 *
	 * @return The first fee fork height.
	 */
	public BlockHeight getFirstHeight() {
		return this.firstHeight;
	}

	/**
	 * Gets the second fee fork height.
	 *
	 * @return The second fee fork height.
	 */
	public BlockHeight getSecondHeight() {
		return this.secondHeight;
	}
}
