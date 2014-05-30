package org.nem.core.model;

import org.nem.nis.AccountAnalyzer;

public class AccountsHeightObserver implements BlockTransferObserver {
	final AccountAnalyzer accountAnalyzer;

	public AccountsHeightObserver(final AccountAnalyzer accountAnalyzer) {
		this.accountAnalyzer = accountAnalyzer;
	}

	private void addToAccountAnalyzer(final BlockHeight height, final Account account) {
		final Account found = this.accountAnalyzer.findByAddress(account.getAddress());

		if (found.getHeight() == null) {
			found.setHeight(height);
		}
	}


	private void tryRemoveFromAccountAnalyzer(final BlockHeight height, final Account account) {
		final Account found = this.accountAnalyzer.findByAddress(account.getAddress());

		if (found != null && found.getHeight() != null) {
			if (found.getHeight().equals(height)) {
				this.accountAnalyzer.removeAccountFromCache(found);
			}

		} else {
			throw new IllegalArgumentException("problem during undo, account not present in AA");
		}
	}

	@Override
	public void notifySend(final BlockHeight height, final Account account, final Amount amount) {
		addToAccountAnalyzer(height, account);
	}

	@Override
	public void notifyReceive(final BlockHeight height, final Account account, final Amount amount) {
		addToAccountAnalyzer(height, account);
	}

	@Override
	public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount) {
		tryRemoveFromAccountAnalyzer(height, account);
	}

	@Override
	public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount) {
		tryRemoveFromAccountAnalyzer(height, account);
	}
}
