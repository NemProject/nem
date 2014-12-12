package org.nem.core.model.observers;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountInfo;

/**
 * Transfer observer that commits balance changes to the underlying accounts.
 */
public class BalanceCommitTransferObserver implements TransferObserver {
	private final AccountStateRepository accountStateRepository;

	/**
	 * Creates an observer.
	 *
	 * @param accountStateRepository The poi facade.
	 */
	public BalanceCommitTransferObserver(final AccountStateRepository accountStateRepository) {
		this.accountStateRepository = accountStateRepository;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		this.notifyDebit(sender, amount);
		this.notifyCredit(recipient, amount);
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		this.getAccountInfo(account).incrementBalance(amount);
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		this.getAccountInfo(account).decrementBalance(amount);
	}

	private AccountInfo getAccountInfo(final Account account) {
		return this.accountStateRepository.findStateByAddress(account.getAddress()).getAccountInfo();
	}
}
