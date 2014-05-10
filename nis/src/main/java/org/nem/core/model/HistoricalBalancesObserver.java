package org.nem.core.model;

/**
 * Block observer that is responsible for updating historical balances
 */
class HistoricalBalancesObserver implements BlockTransferObserver {
	@Override
	public void notifyTransfer(BlockHeight height, Account sender, Account recipient, Amount amount) {
		sender.subtractHistoricalBalance(height, amount);
		recipient.addHistoricalBalance(height, amount);
	}

	@Override
	public void notifyCredit(BlockHeight height, Account account, Amount amount) {
		account.addHistoricalBalance(height, amount);
	}

	@Override
	public void notifyDebit(BlockHeight height, Account account, Amount amount) {
		account.subtractHistoricalBalance(height, amount);
	}
}
