package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.*;

/**
 * Observer that intercepts MultisigModificationNotifications to update an account's multisig links.
 */
public class MultisigAccountObserver implements BlockTransactionObserver {
	private final AccountStateCache stateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public MultisigAccountObserver(final AccountStateCache accountStateCache) {
		this.stateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		// TODO 20150531 J-B: any reason we can't have two observers; one for each type?
		// TODO 20150601 BR -> J: no other reason than the class name suggesting that the original author wanted a general observer
		// > that observes all modifications made to a multsig account. Rename this observer to MultisigCosignatoryModificationObserver
		// > and have a separate MultisigMinCosignatoriesModificationObserver?
		switch (notification.getType()) {
			case CosignatoryModification:
				this.notify((MultisigCosignatoryModificationNotification)notification, context);
				break;
			case MinCosignatoriesModification:
				this.notify((MultisigMinCosignatoriesModificationNotification)notification, context);
				break;
			default:
				break;
		}
	}

	private void notify(final MultisigCosignatoryModificationNotification notification, final BlockNotificationContext context) {
		final Address multisigAddress = notification.getMultisigAccount().getAddress();
		final AccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		final boolean execute = NotificationTrigger.Execute == context.getTrigger();

		final MultisigCosignatoryModification modification = notification.getModification();
		final boolean add = MultisigModificationType.AddCosignatory == modification.getModificationType();
		final Address cosignatoryAddress = modification.getCosignatory().getAddress();
		final AccountState cosignatoryState = this.stateCache.findStateByAddress(cosignatoryAddress);
		final MultisigLinks multisigLinks = multisigState.getMultisigLinks();

		if (add == execute) {
			multisigState.getMultisigLinks().addCosignatory(cosignatoryAddress);
			cosignatoryState.getMultisigLinks().addCosignatoryOf(multisigAddress);
		} else {
			multisigLinks.removeCosignatory(cosignatoryAddress);
			cosignatoryState.getMultisigLinks().removeCosignatoryOf(multisigAddress);
		}
	}

	private void notify(final MultisigMinCosignatoriesModificationNotification notification, final BlockNotificationContext context) {
		final Address multisigAddress = notification.getMultisigAccount().getAddress();
		final AccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		final boolean execute = NotificationTrigger.Execute == context.getTrigger();
		final MultisigMinCosignatoriesModification modification = notification.getModification();

		if (execute) {
			multisigState.getMultisigLinks().incrementMinCosignatoriesBy(modification.getRelativeChange());
		} else {
			multisigState.getMultisigLinks().incrementMinCosignatoriesBy(-modification.getRelativeChange());
		}
	}
}
