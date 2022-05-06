package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.NisCache;
import org.nem.nis.state.AccountState;

/**
 * A BlockTransactionObserver implementation that updates account heights.
 */
public class AccountsHeightObserver implements BlockTransactionObserver {
	private final NisCache nisCache;

	/**
	 * Creates a new observer.
	 *
	 * @param nisCache The NIS cache to use.
	 */
	public AccountsHeightObserver(final NisCache nisCache) {
		this.nisCache = nisCache;
	}

	@Override
	public void notify(final Notification notification, final BlockNotificationContext context) {
		if (NotificationType.Account != notification.getType()) {
			return;
		}

		final Account account = ((AccountNotification) (notification)).getAccount();
		if (NotificationTrigger.Execute == context.getTrigger()) {
			this.addToNisCache(context.getHeight(), account);
		} else {
			this.tryRemoveFromAccountAnalyzer(account);
		}
	}

	private void addToNisCache(final BlockHeight height, final Account account) {
		final Address address = account.getAddress();
		this.nisCache.getAccountCache().addAccountToCache(address);
		final AccountState accountState = this.nisCache.getAccountStateCache().findStateByAddress(address);

		accountState.getAccountInfo().incrementReferenceCount();
		accountState.setHeight(height);
	}

	private void tryRemoveFromAccountAnalyzer(final Account account) {
		final Address address = account.getAddress();
		final Account cachedAccount = this.nisCache.getAccountCache().findByAddress(address);
		if (null == cachedAccount) {
			throw new IllegalArgumentException("problem during undo, account not present in cache");
		}

		final AccountState accountState = this.nisCache.getAccountStateCache().findStateByAddress(address);
		if (null == accountState.getHeight()) {
			throw new IllegalArgumentException("problem during undo, account height not set");
		}

		if (ReferenceCount.ZERO.equals(accountState.getAccountInfo().decrementReferenceCount())) {
			this.nisCache.getAccountCache().removeFromCache(address);
			this.nisCache.getAccountStateCache().removeFromCache(address);
		}
	}
}
