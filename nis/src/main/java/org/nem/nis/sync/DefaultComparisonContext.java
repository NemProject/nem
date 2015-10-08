package org.nem.nis.sync;

import org.nem.core.model.NemGlobals;
import org.nem.core.model.primitive.BlockHeight;

/**
 * A default comparison context that is populated with block chain constants.
 */
public class DefaultComparisonContext extends ComparisonContext {

	/**
	 * Creates a default comparison context.
	 *
	 * @param height The local block height.
	 */
	public DefaultComparisonContext(final BlockHeight height) {
		super(getAnalyzeLimitAtHeight(height), NemGlobals.getBlockChainConfiguration().getBlockChainRewriteLimit());
	}

	@SuppressWarnings("UnusedParameters")
	private static int getAnalyzeLimitAtHeight(final BlockHeight height) {
		return NemGlobals.getBlockChainConfiguration().getMaxBlocksPerSyncAttempt();
	}
}
