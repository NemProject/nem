package org.nem.nis.sync;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.secret.BlockChainConstants;

/**
 * A default comparison context that is populated with block chain constants.
 */
public class DefaultComparisonContext extends ComparisonContext {
	private static final int OLD_REWRITE_LIMIT = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY / 2;

	/**
	 * Creates a default comparison context.
	 *
	 * @param height The local block height.
	 */
	public DefaultComparisonContext(final BlockHeight height) {
		super(BlockChainConstants.BLOCKS_LIMIT, getRewriteLimitAtHeight(height));
	}

	private static int getRewriteLimitAtHeight(final BlockHeight height) {
		final long testHeight = height.getRaw() + BlockChainConstants.BLOCKS_LIMIT;
		return testHeight > BlockMarkerConstants.BETA_HARD_FORK
				? BlockChainConstants.REWRITE_LIMIT
				: OLD_REWRITE_LIMIT;
	}
}
