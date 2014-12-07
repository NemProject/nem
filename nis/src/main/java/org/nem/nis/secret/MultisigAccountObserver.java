package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.nis.poi.*;

public class MultisigAccountObserver implements BlockTransactionObserver {
	private final PoiFacade poiFacade;

	public MultisigAccountObserver(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
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
		final PoiAccountState multisigState = this.poiFacade.findStateByAddress(multisigAddress);
		boolean execute = NotificationTrigger.Execute == context.getTrigger();

		for (final MultisigModification modification : notification.getModifications()) {
			boolean add = MultisigModificationType.Add == modification.getModificationType();
			final Address cosignatoryAddress = modification.getCosignatory().getAddress();
			final PoiAccountState cosignatoryState = this.poiFacade.findStateByAddress(cosignatoryAddress);

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
