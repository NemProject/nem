package org.nem.nis.secret.pruning;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.NisCache;
import org.nem.nis.secret.BlockTransactionObserver;
import org.nem.nis.test.NisTestConstants;

public class NamespaceCachePruningObserverTest extends AbstractPruningObserverTest {
	private static final long NAMESPACE_BLOCK_HISTORY = NisTestConstants.ESTIMATED_BLOCKS_PER_DAY * (365 + 30 + 1);

	// region overrides

	@Override
	protected BlockTransactionObserver createObserver(final NisCache nisCache) {
		return new NamespaceCachePruningObserver(nisCache.getNamespaceCache());
	}

	@Override
	protected void assertPruning(final NisCache nisCache, final long state) {
		Mockito.verify(nisCache.getNamespaceCache(), Mockito.only()).prune(new BlockHeight(state));
	}

	@Override
	protected void assertNoPruning(final NisCache nisCache) {
		Mockito.verify(nisCache.getNamespaceCache(), Mockito.never()).prune(Mockito.any());
	}

	// endregion

	@Test
	public void blockBasedPruningIsTriggeredAtInitialBlockHeight() {
		// Assert:
		this.assertBlockBasedPruning(1, 1);
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsNearNamespaceBlockHistory() {
		// Assert: state is expected pruned height
		this.assertBlockBasedPruning(NAMESPACE_BLOCK_HISTORY - 2 * PRUNE_INTERVAL + 1, 1);
		this.assertBlockBasedPruning(NAMESPACE_BLOCK_HISTORY - PRUNE_INTERVAL + 1, 1);
		this.assertBlockBasedPruning(NAMESPACE_BLOCK_HISTORY + 1, 1);
		this.assertBlockBasedPruning(NAMESPACE_BLOCK_HISTORY + PRUNE_INTERVAL + 1, 361);
		this.assertBlockBasedPruning(NAMESPACE_BLOCK_HISTORY + 2 * PRUNE_INTERVAL + 1, 721);
	}

	@Test
	public void blockBasedPruningIsTriggeredWhenBlockHeightIsMuchGreaterThanHistory() {
		// Assert:
		this.assertBlockBasedPruning(100 * NAMESPACE_BLOCK_HISTORY + 1, 99 * NAMESPACE_BLOCK_HISTORY + 1);
	}
}
