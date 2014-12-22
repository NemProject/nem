package org.nem.nis.secret;

import org.nem.core.model.observers.*;
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
		// TODO 20141222 BR _> J: Can we either have an individual notification type for each observer or at least have a more general naming for block notifications?
		if (NotificationType.HarvestReward != notification.getType()) {
			return;
		}

		this.nisCache.getPoiFacade().recalculateImportances(
				context.getHeight(),
				this.nisCache.getAccountStateCache().mutableContents().asCollection());
	}
}
