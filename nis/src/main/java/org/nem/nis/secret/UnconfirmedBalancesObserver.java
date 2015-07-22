package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.observers.TransferObserver;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.ReadOnlyAccountStateCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An observer that updates unconfirmed balance information
 */
public class UnconfirmedBalancesObserver implements TransferObserver {
	private final ReadOnlyAccountStateCache accountStateCache;
	private final Map<Account, Amount> creditedAmounts = new ConcurrentHashMap<>();
	private final Map<Account, Amount> debitedAmounts = new ConcurrentHashMap<>();

	/**
	 * Creates an unconfirmed balances observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public UnconfirmedBalancesObserver(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	/**
	 * Gets the (unconfirmed) balance of the specified account.
	 *
	 * @param account The account.
	 * @return The balance.
	 */
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
		// if, for some reason, a transaction got validated but meanwhile the confirmed balance changed,
		// it's probably better to let an exception bubble out here (the following line will throw
		// if the new unconfirmed balance is negative)
		this.getBalance(account).add(this.getCreditedAmount(account)).subtract(newDebitedAmount);
		this.debitedAmounts.replace(account, newDebitedAmount);
	}

	private void addToCache(final Account account) {
		// it's ok to put reference here, thanks to Account being non-mutable
		this.creditedAmounts.putIfAbsent(account, Amount.ZERO);
		this.debitedAmounts.putIfAbsent(account, Amount.ZERO);
	}

	/**
	 * Clears the cache.
	 */
	public void clearCache() {
		this.creditedAmounts.clear();
		this.debitedAmounts.clear();
	}

	/**
	 * Gets a value indicating whether or not all unconfirmed balances are valid (positive).
	 *
	 * @return true if all unconfirmed balances are valid.
	 */
	public boolean unconfirmedBalancesAreValid() {
		return this.creditedAmounts.keySet().stream()
				.allMatch(account -> this.getBalance(account).add(this.getCreditedAmount(account)).compareTo(this.getDebitedAmount(account)) >= 0);
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
