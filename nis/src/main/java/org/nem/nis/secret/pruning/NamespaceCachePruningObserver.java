package org.nem.nis.secret.pruning;

import org.nem.core.model.NemGlobals;
import org.nem.nis.cache.NamespaceCache;
import org.nem.nis.secret.BlockNotificationContext;

/**
 * A block transaction observer that automatically prunes namespace-related data once every 360 blocks.
 */
public class NamespaceCachePruningObserver extends AbstractPruningObserver {
	private final NamespaceCache namespaceCache;

	/**
	 * Creates a new observer.
	 *
	 * @param namespaceCache The namespace cache.
	 */
	public NamespaceCachePruningObserver(final NamespaceCache namespaceCache) {
		this.namespaceCache = namespaceCache;
	}

	@Override
	protected void prune(final BlockNotificationContext context) {
		final long namespaceBlockHistory = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerDay() * (365 + 30 + 1);
		this.namespaceCache.prune(getPruneHeight(context.getHeight(), namespaceBlockHistory));
	}
}
