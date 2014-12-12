package org.nem.nis.secret;

import org.apache.commons.collections4.IteratorUtils;
import org.nem.core.model.observers.*;
import org.nem.nis.BlockScorer;
import org.nem.nis.cache.NisCache;

/**
 * An observer that recalculates POI importances.
 */
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

		this.nisCache.getPoiFacade().recalculateImportances(
				BlockScorer.getGroupedHeight(context.getHeight()),
				IteratorUtils.toList(this.nisCache.getAccountStateCache().iterator()));
	}
}
