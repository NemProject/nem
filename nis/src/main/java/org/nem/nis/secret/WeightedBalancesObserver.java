package org.nem.nis.secret;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.nis.cache.PoiFacade;
import org.nem.nis.state.WeightedBalances;

/**
 * An observer that updates weighted balance information.
 */
public class WeightedBalancesObserver implements BlockTransferObserver {
	private final PoiFacade poiFacade;

	/**
	 * Creates a new weighted balances observer.
	 *
	 * @param poiFacade The poi facade.
	 */
	public WeightedBalancesObserver(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
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
		return this.poiFacade.findStateByAddress(account.getAddress()).getWeightedBalances();
	}
}
