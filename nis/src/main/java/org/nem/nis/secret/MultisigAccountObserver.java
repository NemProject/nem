package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

public class MultisigAccountObserver implements BlockTransactionObserver {
	private final AccountStateCache stateCache;

	public MultisigAccountObserver(final AccountStateCache accountStateCache) {
		this.stateCache = accountStateCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.CosignatoryModification) {
			return;
		}

		this.notify((MultisigModificationNotification)notification, context);
	}

	private void notify(final MultisigModificationNotification notification, final BlockNotificationContext context) {
		final Address multisigAddress = notification.getMultisigAccount().getAddress();
		final AccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		boolean execute = NotificationTrigger.Execute == context.getTrigger();

		for (final MultisigModification modification : notification.getModifications()) {
			boolean add = MultisigModificationType.Add == modification.getModificationType();
			final Address cosignatoryAddress = modification.getCosignatory().getAddress();
			final AccountState cosignatoryState = this.stateCache.findStateByAddress(cosignatoryAddress);

			if ((add && execute) || (!add && !execute)) {
				multisigState.getMultisigLinks().addCosignatory(cosignatoryAddress, context.getHeight());
				cosignatoryState.getMultisigLinks().addMultisig(multisigAddress, context.getHeight());
			} else {
				multisigState.getMultisigLinks().removeCosignatory(cosignatoryAddress, context.getHeight());
				cosignatoryState.getMultisigLinks().removeMultisig(multisigAddress, context.getHeight());
			}
		}

	}
}
