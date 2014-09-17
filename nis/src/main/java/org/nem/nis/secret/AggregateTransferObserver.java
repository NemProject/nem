package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.Amount;

import java.util.List;

/**
 * An aggregate transfer observer.
 */
public class AggregateTransferObserver extends TransactionObserverToTransferObserverAdapter {

	private final List<TransferObserver> transferObservers;

	/**
	 * Creates a new aggregate.
	 *
	 * @param transferObservers The sub transfer observers.
	 */
	public AggregateTransferObserver(final List<TransferObserver> transferObservers) {
		this.transferObservers = transferObservers;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		for (final TransferObserver o : this.transferObservers) {
			o.notifyTransfer(sender, recipient, amount);
		}
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		for (final TransferObserver o : this.transferObservers) {
			o.notifyCredit(account, amount);
		}
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		for (final TransferObserver o : this.transferObservers) {
			o.notifyDebit(account, amount);
		}
	}
}
