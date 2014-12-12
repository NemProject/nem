package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.WeightedBalances;

/**
 * An observer that updates weighted balance information.
 */
public class WeightedBalancesObserver implements BlockTransferObserver {
	private final AccountStateCache accountStateCache;

	/**
	 * Creates a new weighted balances observer.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public WeightedBalancesObserver(final AccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	// keep in mind this is called TWICE for every transaction:
	// once with amount and once with fee
	@Override
	public void notifySend(final BlockHeight height, final Account account, final Amount amount) {
		this.getWeightedBalances(account).addSend(height, amount);
	}

	@Override
	public void notifyReceive(final BlockHeight height, final Account account, final Amount amount) {
		this.getWeightedBalances(account).addReceive(height, amount);
	}

	// keep in mind this is called TWICE for every transaction:
	// once with amount and once with fee
	@Override
	public void notifySendUndo(final BlockHeight height, final Account account, final Amount amount) {
		this.getWeightedBalances(account).undoSend(height, amount);
	}

	@Override
	public void notifyReceiveUndo(final BlockHeight height, final Account account, final Amount amount) {
		this.getWeightedBalances(account).undoReceive(height, amount);
	}

	private WeightedBalances getWeightedBalances(final Account account) {
		return this.accountStateCache.findStateByAddress(account.getAddress()).getWeightedBalances();
	}
}
