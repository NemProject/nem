package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;

/**
 * Transfer observer that commits balance changes to the underlying accounts.
 */
public class BalanceCommitTransferObserver implements TransferObserver {

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		this.notifyDebit(sender, amount);
		this.notifyCredit(recipient, amount);
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		account.incrementBalance(amount);
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		account.decrementBalance(amount);
	}
}
