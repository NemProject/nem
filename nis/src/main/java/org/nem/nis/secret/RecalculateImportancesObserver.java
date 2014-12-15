package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.BlockScorer;
import org.nem.nis.cache.NisCache;
import org.nem.nis.poi.GroupedHeight;

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

		// TODO 20141212: this is a hack
		if (context.getHeight().equals(BlockHeight.ONE)) {
			return;
		}

		this.nisCache.getPoiFacade().recalculateImportances(
				context.getHeight(),
				this.nisCache.getAccountStateCache().mutableContents().asCollection());
	}
}
