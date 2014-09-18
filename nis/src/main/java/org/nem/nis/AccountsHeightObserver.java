package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.secret.*;

/**
 * A BlockTransactionObserver implementation that updates account heights.
 */
public class AccountsHeightObserver implements BlockTransactionObserver {
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
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.Account != notification.getType()) {
			return;
		}

		final Account account = ((AccountNotification)(notification)).getAccount();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.addToAccountAnalyzer(context.getHeight(), account);
		} else {
			this.tryRemoveFromAccountAnalyzer(account);
		}
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
