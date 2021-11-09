package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

/**
 * Observer that intercepts MultisigModificationNotifications to update an account's multisig links.
 */
public class MultisigCosignatoryModificationObserver implements BlockTransactionObserver {
	private final AccountStateCache stateCache;

	/**
	 * Creates a new observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public MultisigCosignatoryModificationObserver(final AccountStateCache accountStateCache) {
		this.stateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.CosignatoryModification) {
			return;
		}

		this.notify((MultisigCosignatoryModificationNotification) notification, context);
	}

	private void notify(final MultisigCosignatoryModificationNotification notification, final BlockNotificationContext context) {
		final Address multisigAddress = notification.getMultisigAccount().getAddress();
		final AccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		final boolean execute = NotificationTrigger.Execute == context.getTrigger();

		final MultisigCosignatoryModification modification = notification.getModification();
		final boolean add = MultisigModificationType.AddCosignatory == modification.getModificationType();
		final Address cosignatoryAddress = modification.getCosignatory().getAddress();
		final AccountState cosignatoryState = this.stateCache.findStateByAddress(cosignatoryAddress);

		if (add == execute) {
			multisigState.getMultisigLinks().addCosignatory(cosignatoryAddress);
			cosignatoryState.getMultisigLinks().addCosignatoryOf(multisigAddress);
		} else {
			multisigState.getMultisigLinks().removeCosignatory(cosignatoryAddress);
			cosignatoryState.getMultisigLinks().removeCosignatoryOf(multisigAddress);
		}
	}
}
