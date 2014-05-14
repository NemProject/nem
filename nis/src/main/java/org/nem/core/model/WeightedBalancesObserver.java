package org.nem.core.model;

public class WeightedBalancesObserver implements BlockTransferObserver {

	// keep in mind this is called TWICE for every transaction:
	// once with amount and once with fee
	@Override
	public void notifySend(final BlockHeight height, final Account account, final Amount amount) {
		account.weightedSend(height, amount);
	}

	@Override
	public void notifyReceive(final BlockHeight height, final Account account, final Amount amount) {
		account.weightedReceive(height, amount);
	}

	// keep in mind this is called TWICE for every transaction:
	// once with amount and once with fee
	@Override
	public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount) {
		account.weightedSendUndo(height, amount);
	}

	@Override
	public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount) {
		account.weightedReceiveUndo(height, amount);
	}
}
