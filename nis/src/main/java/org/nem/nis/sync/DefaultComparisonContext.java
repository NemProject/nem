package org.nem.nis.sync;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.*;

/**
 * A default comparison context that is populated with block chain constants.
 */
public class DefaultComparisonContext extends ComparisonContext {
	private static final int OLD_ANALYZE_LIMIT = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

	/**
	 * Creates a default comparison context.
	 *
	 * @param height The local block height.
	 */
	public DefaultComparisonContext(final BlockHeight height) {
		super(getAnalyzeLimitAtHeight(height), BlockChainConstants.REWRITE_LIMIT);
	}

	private static int getAnalyzeLimitAtHeight(final BlockHeight height) {
		final long testHeight = height.getRaw() + BlockChainConstants.BLOCKS_LIMIT;
		return testHeight > BlockMarkerConstants.BETA_HARD_FORK
				? BlockChainConstants.BLOCKS_LIMIT
				: OLD_ANALYZE_LIMIT;
	}
}
