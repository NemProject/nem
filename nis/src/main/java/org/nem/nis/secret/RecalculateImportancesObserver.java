package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockScorer;
import org.nem.nis.cache.NisCache;

/**
 * An observer that recalculates POI importances.
 */
// TODO 20141213 G-J: introduction of this, causes, that poi is recalculated also during loading of blockchain
// earlier recalculation was done after all the blocks have been loaded,
// (for 70k blocks that is >180 recalculations)
// I don't think that it's important as it's only fraction of time, just wanted to note the difference with previous behaviour
public class RecalculateImportancesObserver implements BlockTransactionObserver {
	private final NisCache nisCache;

	/**
	 * Creates a new observer.
	 *
	 * @param nisCache The NIS cache.
	 */
	public RecalculateImportancesObserver(final NisCache nisCache) {
		this.nisCache = nisCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.HarvestReward != notification.getType()) {
			return;
		}

		// TODO 20141212: this is a hack
		if (context.getHeight().equals(BlockHeight.ONE)) {
			return;
		}

		this.nisCache.getPoiFacade().recalculateImportances(
				BlockScorer.getGroupedHeight(context.getHeight()),
				this.nisCache.getAccountStateCache().mutableContents().asCollection());
	}
}
