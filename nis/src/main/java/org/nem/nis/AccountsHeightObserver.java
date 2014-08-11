package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.secret.BlockTransferObserver;

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
		this.tryRemoveFromAccountAnalyzer(account);
	}

	private void addToAccountAnalyzer(final BlockHeight height, final Account account) {
		final Address address = account.getAddress();
		final Account cachedAccount = this.accountAnalyzer.getAccountCache().findByAddress(address);
		final PoiAccountState accountState = this.accountAnalyzer.getPoiFacade().findStateByAddress(address);

		cachedAccount.incrementReferenceCount();
		accountState.setHeight(height);
	}

	private void tryRemoveFromAccountAnalyzer(final Account account) {
		final Address address = account.getAddress();
		final Account cachedAccount = this.accountAnalyzer.getAccountCache().findByAddress(address);
		if (null == cachedAccount) {
			throw new IllegalArgumentException("problem during undo, account not present in cache");
		}

		final PoiAccountState accountState = this.accountAnalyzer.getPoiFacade().findStateByAddress(address);
		if (null == accountState.getHeight()) {
			throw new IllegalArgumentException("problem during undo, account height not set");
		}

		if (ReferenceCount.ZERO.equals(cachedAccount.decrementReferenceCount())) {
			this.accountAnalyzer.removeAccount(address);
		}
	}
}
