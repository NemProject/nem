package org.nem.nis.secret;

import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.cache.*;
import org.nem.nis.pox.poi.GroupedHeight;
import org.nem.nis.state.AccountState;

import java.util.Collection;

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

		final PoxFacade poxFacade = this.nisCache.getPoxFacade();
		final BlockHeight lastRecalculationHeight = poxFacade.getLastRecalculationHeight();
		final BlockHeight effectiveHeight = NotificationTrigger.Execute == context.getTrigger()
				? context.getHeight().next()
				: context.getHeight();
		if (null != lastRecalculationHeight && 0 == lastRecalculationHeight.compareTo(GroupedHeight.fromHeight(effectiveHeight))) {
			return;
		}

		final Collection<AccountState> accountStates = this.nisCache.getAccountStateCache().mutableContents().asCollection();
		poxFacade.recalculateImportances(effectiveHeight, accountStates);
	}
}
