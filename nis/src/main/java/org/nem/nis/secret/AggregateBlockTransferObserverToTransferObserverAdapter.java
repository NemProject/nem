package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;

import java.util.List;

/**
 * An adapter from an aggregate BlockTransferObserver to a TransferObserver.
 */
public class AggregateBlockTransferObserverToTransferObserverAdapter implements TransferObserver {

	private final List<BlockTransferObserver> blockTransferObservers;
	private final BlockHeight height;
	private final boolean isExecute;

	/**
	 * Creates a new adapter.
	 *
	 * @param blockTransferObservers The block transfer observers.
	 * @param height The block height.
	 * @param isExecute true if the transfers represent an execute; false if they represent an undo.
	 */
	public AggregateBlockTransferObserverToTransferObserverAdapter(
			final List<BlockTransferObserver> blockTransferObservers,
			final BlockHeight height,
			final boolean isExecute) {
		this.blockTransferObservers = blockTransferObservers;
		this.height = height;
		this.isExecute = isExecute;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		this.notifyCredit(recipient, amount);
		this.notifyDebit(sender, amount);
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		for (final BlockTransferObserver o : this.blockTransferObservers) {
			if (this.isExecute) {
				o.notifyReceive(this.height, account, amount);
			} else {
				o.notifySendUndo(this.height, account, amount);
			}
		}
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		for (final BlockTransferObserver o : this.blockTransferObservers) {
			if (this.isExecute) {
				o.notifySend(this.height, account, amount);
			} else {
				o.notifyReceiveUndo(this.height, account, amount);
			}
		}
	}
}
