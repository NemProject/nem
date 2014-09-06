package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.poi.*;

/**
 * A transfer observer that updates outlink information.
 */
public class RemoteObserver implements ImportanceTransferObserver {
	private final PoiFacade poiFacade;
	private final BlockHeight height;
	private final boolean isExecute;

	/**
	 * Creates a new observer.
	 *
	 * @param poiFacade The poi facade.
	 * @param height The block height.
	 * @param isExecute true if the transfers represent an execute; false if they represent an undo.
	 */
	public RemoteObserver(final PoiFacade poiFacade, final BlockHeight height, final boolean isExecute) {
		this.poiFacade = poiFacade;
		this.height = height;
		this.isExecute = isExecute;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final int direction) {
		if (this.isExecute) {
			if (direction == ImportanceTransferTransactionDirection.Transfer) {
				this.getState(sender).setRemote(recipient.getAddress(), this.height, direction);
				this.getState(recipient).remoteFor(sender.getAddress(), this.height, direction);
			} else if (direction == ImportanceTransferTransactionDirection.Revert) {
				this.getState(sender).setRemote(null, this.height, direction);
				this.getState(recipient).setRemote(null, this.height, direction);
			}
		} else {
			if (direction == ImportanceTransferTransactionDirection.Transfer) {
				this.getState(recipient).resetRemote(sender.getAddress(), this.height, direction);
				this.getState(sender).resetRemote(recipient.getAddress(), this.height, direction);
			} else if (direction == ImportanceTransferTransactionDirection.Revert) {
				this.getState(recipient).resetRemote(null, this.height, direction);
				this.getState(sender).resetRemote(null, this.height, direction);
			}
		}
	}

	private PoiAccountState getState(final Account account) {
		return this.poiFacade.findStateByAddress(account.getAddress());
	}
}
