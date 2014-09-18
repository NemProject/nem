package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.ImportanceTransferNotification;
import org.nem.core.model.observers.Notification;
import org.nem.core.model.observers.NotificationType;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.*;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.nem.nis.poi.*;

/**
 * A transfer observer that updates outlink information.
 */
public class RemoteObserver implements BlockTransactionObserver {
	private final PoiFacade poiFacade;

	/**
	 * Creates a new observer.
	 *
	 * @param poiFacade The poi facade.
	 */
	public RemoteObserver(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	private PoiAccountState getState(final Account account) {
		return this.poiFacade.findStateByAddress(account.getAddress());
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (notification.getType() != NotificationType.ImportanceTransfer) {
			return;
		}

		final ImportanceTransferNotification itn = (ImportanceTransferNotification)notification;
		final Account sender = itn.getLessor();
		final Account recipient = itn.getLessee();
		final int mode = itn.getMode();

		if (context.getTrigger() == NotificationTrigger.Execute) {
			this.getState(sender).setRemote(recipient.getAddress(), context.getHeight(), mode);
			this.getState(recipient).remoteFor(sender.getAddress(), context.getHeight(), mode);
		} else {
			this.getState(recipient).resetRemote(sender.getAddress(), context.getHeight(), mode);
			this.getState(sender).resetRemote(recipient.getAddress(), context.getHeight(), mode);
		}
	}
}
