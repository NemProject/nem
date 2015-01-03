package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;

// TODO 20150103 J-G: please comment

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

		// TODO 20150103 J-G: does it make sense to notify each MultisigModification individually; so the notification doesn't have a list;
		// > not sure; just a thought
		this.notify((MultisigModificationNotification)notification, context);
	}

	private void notify(final MultisigModificationNotification notification, final BlockNotificationContext context) {
		final Address multisigAddress = notification.getMultisigAccount().getAddress();
		final AccountState multisigState = this.stateCache.findStateByAddress(multisigAddress);
		final boolean execute = NotificationTrigger.Execute == context.getTrigger();

		for (final MultisigModification modification : notification.getModifications()) {
			final boolean add = MultisigModificationType.Add == modification.getModificationType();
			final Address cosignatoryAddress = modification.getCosignatory().getAddress();
			final AccountState cosignatoryState = this.stateCache.findStateByAddress(cosignatoryAddress);

			// TODO 20150103 J-G: maybe more confusing, but equivalent to add == execute
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
