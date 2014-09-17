package org.nem.nis.secret;

import org.nem.core.model.*;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.poi.*;

import java.math.BigInteger;

/**
 * A transfer observer that updates outlink information.
 */
public class OutlinkObserver implements TransferObserver {
	private final PoiFacade poiFacade;
	private final BlockHeight height;
	private final boolean isExecute;

	/**
	 * Creates a new observer.
	 *
	 * @param poiFacade The poi facade.
	 * @param height The block height.
	 * @param isExecute true if the transfers represent an execute; false if they represent an undo.
	 */
	public OutlinkObserver(final PoiFacade poiFacade, final BlockHeight height, final boolean isExecute) {
		this.poiFacade = poiFacade;
		this.height = height;
		this.isExecute = isExecute;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		// Trying to gain importance by sending nem to yourself?
		if (sender.getAddress().equals(recipient.getAddress())) {
			return;
		}

		final Amount linkWeight = this.calculateLinkWeight(this.isExecute ? sender : recipient, amount);

		if (this.isExecute) {
			final AccountLink link = new AccountLink(this.height, linkWeight, recipient.getAddress());
			this.getState(sender).getImportanceInfo().addOutlink(link);
		} else {
			final AccountLink link = new AccountLink(this.height, linkWeight, sender.getAddress());
			this.getState(recipient).getImportanceInfo().removeOutlink(link);
		}
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
	}

	private Amount calculateLinkWeight(final Account sender, final Amount amount) {
		final WeightedBalances weightedBalances = this.getState(sender).getWeightedBalances();
		final BigInteger vested = BigInteger.valueOf(getNumMicroNem(weightedBalances.getVested(this.height)));
		final BigInteger unvested = BigInteger.valueOf(getNumMicroNem(weightedBalances.getUnvested(this.height)));
		if (unvested.compareTo(BigInteger.ZERO) <= 0) {
			return amount;
		}

		// only use the vested portion of an account's balance in outlink determination
		final long rawAdjustedWeight = BigInteger.valueOf(amount.getNumMicroNem())
				.multiply(vested)
				.divide(vested.add(unvested))
				.longValue();
		return Amount.fromMicroNem(rawAdjustedWeight);
	}

	private PoiAccountState getState(final Account account) {
		return this.poiFacade.findStateByAddress(account.getAddress());
	}

	private static long getNumMicroNem(final Amount amount) {
		return null == amount ? 0 : amount.getNumMicroNem();
	}
}
