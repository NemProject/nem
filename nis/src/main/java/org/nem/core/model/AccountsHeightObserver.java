package org.nem.core.model;

import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.AccountAnalyzer;

/**
 * A BlockTransferObserver implementation that updates account heights.
 */
public class AccountsHeightObserver implements BlockTransferObserver {
	final AccountAnalyzer accountAnalyzer;

	/**
	 * Creates a new observer.
	 *
	 * @param accountAnalyzer The account analyzer to use.
	 */
	public AccountsHeightObserver(final AccountAnalyzer accountAnalyzer) {
		this.accountAnalyzer = accountAnalyzer;
	}

	@Override
	public void notifySend(final BlockHeight height, final Account account, final Amount amount) {
		// no need to do anything here
	}

	@Override
	public void notifyReceive(final BlockHeight height, final Account account, final Amount amount) {
		this.addToAccountAnalyzer(height, account);
	}

	@Override
	public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount) {
		// no need to do anything here
	}

	@Override
	public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount) {
		this.tryRemoveFromAccountAnalyzer(height, account);
	}

	private void addToAccountAnalyzer(final BlockHeight height, final Account account) {
		final Account found = this.accountAnalyzer.findByAddress(account.getAddress());
		found.setHeight(height);
		found.incrementReferenceCounter();
	}

	private void tryRemoveFromAccountAnalyzer(final BlockHeight height, final Account account) {
		final Address address = account.getAddress();
		final Account found = this.accountAnalyzer.findByAddress(address);

		if (null == found || null == found.getHeight())
			throw new IllegalArgumentException("problem during undo, account not present in AA or account height is null");

		if (found.decrementReferenceCounter().getRaw() == 0) {
			this.accountAnalyzer.removeAccountFromCache(address);
		}
	}
}
