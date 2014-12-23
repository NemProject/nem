package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.TransferObserver;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.ReadOnlyAccountStateCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO 20141218 J-B: changes look pretty good, nice job; i have a few comments:
// > it might make sense to pull UnconfirmedBalancesObserver out into its own class so we can test it more directly
// > should definitely add tests for the cases that trigger the cache rebuild (since this is presumably the source of our bugs)
/**
 * An observer that updates unconfirmed balance information
 */
public class UnconfirmedBalancesObserver implements TransferObserver {
	private final ReadOnlyAccountStateCache accountStateCache;
	private final Map<Account, Amount> creditedAmounts = new ConcurrentHashMap<>();
	private final Map<Account, Amount> debitedAmounts = new ConcurrentHashMap<>();

	public UnconfirmedBalancesObserver(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	public Amount get(final Account account) {
		return this.getBalance(account).add(this.getCreditedAmount(account)).subtract(this.getDebitedAmount(account));
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		this.notifyDebit(sender, amount);
		this.notifyCredit(recipient, amount);
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
		this.addToCache(account);
		final Amount newCreditedAmount = this.getCreditedAmount(account).add(amount);
		this.creditedAmounts.replace(account, newCreditedAmount);
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
		this.addToCache(account);
		final Amount newDebitedAmount = this.getDebitedAmount(account).add(amount);
		// should not be necessary but do it anyway as check
		this.getBalance(account).add(this.getCreditedAmount(account)).subtract(newDebitedAmount);
		this.debitedAmounts.replace(account, newDebitedAmount);
	}

	private void addToCache(final Account account) {
		// it's ok to put reference here, thanks to Account being non-mutable
		this.creditedAmounts.putIfAbsent(account, Amount.ZERO);
		this.debitedAmounts.putIfAbsent(account, Amount.ZERO);
	}

	public void clearCache() {
		this.creditedAmounts.clear();
		this.debitedAmounts.clear();
	}

	public boolean unconfirmedBalancesAreValid() {
		for (final Account account : this.creditedAmounts.keySet()) {
			if (this.getBalance(account).add(this.getCreditedAmount(account)).compareTo(this.getDebitedAmount(account)) < 0) {
				return false;
			}
		}

		return true;
	}

	private Amount getBalance(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
	}

	private Amount getCreditedAmount(final Account account) {
		return this.creditedAmounts.getOrDefault(account, Amount.ZERO);
	}

	private Amount getDebitedAmount(final Account account) {
		return this.debitedAmounts.getOrDefault(account, Amount.ZERO);
	}
}
