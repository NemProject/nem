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
		if (NotificationType.BlockHarvest != notification.getType()) {
			return;
		}

		this.nisCache.getPoxFacade().recalculateImportances(
				NotificationTrigger.Execute == context.getTrigger() ? context.getHeight().next() : context.getHeight(),
				this.nisCache.getAccountStateCache().mutableContents().asCollection());
	}
}
