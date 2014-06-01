package org.nem.core.model;

import java.math.BigInteger;

/**
 * A transfer observer that updates outlink information.
 */
public class OutlinkObserver implements TransferObserver {

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
		Amount linkWeight = amount;
		Account trueSender = this.isExecute? sender : recipient;
		BigInteger vested = BigInteger.valueOf(trueSender.getWeightedBalances().getVested(this.height).getNumMicroNem());
		BigInteger unvested = BigInteger.valueOf(trueSender.getWeightedBalances().getUnvested(this.height).getNumMicroNem());
		if (unvested.compareTo(BigInteger.ZERO) > 0) {
			linkWeight = Amount.fromMicroNem(BigInteger.valueOf(amount.getNumMicroNem())
											 .multiply(vested)
											 .divide(vested.add(unvested))
											 .longValue());
		}
		if (this.isExecute) {
			final AccountLink link = new AccountLink(this.height, linkWeight, recipient.getAddress());
			sender.getImportanceInfo().addOutlink(link);
		}
		else {
			final AccountLink link = new AccountLink(this.height, linkWeight, sender.getAddress());
			recipient.getImportanceInfo().removeOutlink(link);
		}
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
	}
}
