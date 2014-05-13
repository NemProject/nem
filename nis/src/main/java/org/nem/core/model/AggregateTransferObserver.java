package org.nem.core.model;

import java.util.List;

/**
 * An aggregate transfer observer.
 */
public class AggregateTransferObserver implements TransferObserver {

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
		transferObservers.stream().forEach(o -> o.notifyTransfer(sender, recipient, amount));
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		transferObservers.stream().forEach(o -> o.notifyCredit(account, amount));
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		transferObservers.stream().forEach(o -> o.notifyDebit(account, amount));
	}
}
