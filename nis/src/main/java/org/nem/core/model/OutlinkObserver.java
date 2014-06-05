package org.nem.core.model;

import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * A transfer observer that updates outlink information.
 */
public class OutlinkObserver implements TransferObserver {
	private static final Logger LOGGER = Logger.getLogger(OutlinkObserver.class.getName());

	private final BlockHeight height;
	private final boolean isExecute;

	/**
	 * Creates a new observer.
	 *
	 * @param height The block height.
	 * @param isExecute true if the transfers represent an execute; false if they represent an undo.
	 */
	public OutlinkObserver(final BlockHeight height, final boolean isExecute) {
		this.height = height;
		this.isExecute = isExecute;
	}

	@Override
	public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
		final Amount linkWeight = calculateLinkWeight(this.isExecute ? sender : recipient, amount);

		if (this.isExecute) {
			final AccountLink link = new AccountLink(this.height, linkWeight, recipient.getAddress());
			sender.getImportanceInfo().addOutlink(link);
		}
		else {
			final AccountLink link = new AccountLink(this.height, linkWeight, sender.getAddress());
			recipient.getImportanceInfo().removeOutlink(link);
		}
	}

	private Amount calculateLinkWeight(final Account sender, final Amount amount) {
		final WeightedBalances weightedBalances = sender.getWeightedBalances();
		final BigInteger vested = BigInteger.valueOf(getNumMicroNem(weightedBalances.getVested(this.height)));
		final BigInteger unvested = BigInteger.valueOf(getNumMicroNem(weightedBalances.getUnvested(this.height)));
		if (unvested.compareTo(BigInteger.ZERO) <= 0)
			return amount;

		// only use the vested portion of an account's balance in outlink determination
        final long rawAdjustedWeight = BigInteger.valueOf(amount.getNumMicroNem())
                .multiply(vested)
                .divide(vested.add(unvested))
                .longValue();
        return Amount.fromMicroNem(rawAdjustedWeight);
	}

	private static long getNumMicroNem(final Amount amount) {
		return null == amount ? 0 : amount.getNumMicroNem();
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
	}
}
