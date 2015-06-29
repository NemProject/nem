package org.nem.nis.secret.pruning;

import org.nem.nis.cache.HashCache;
import org.nem.nis.secret.BlockNotificationContext;

/**
 * A block transaction observer that automatically prunes transaction hash-related data once every 360 blocks.
 */
public class TransactionHashCachePruningObserver extends AbstractPruningObserver {
	private final HashCache hashCache;

	/**
	 * Creates a new observer.
	 *
	 * @param hashCache The hash cache.
	 */
	public TransactionHashCachePruningObserver(final HashCache hashCache) {
		this.hashCache = hashCache;
	}

	@Override
	protected void prune(final BlockNotificationContext context) {
		this.hashCache.prune(context.getTimeStamp());
	}
}
