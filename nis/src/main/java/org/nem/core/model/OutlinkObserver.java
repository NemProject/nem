package org.nem.core.model;

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
		final AccountLink link = new AccountLink(this.height, amount, recipient.getAddress());
		if (this.isExecute)
			sender.getImportanceInfo().addOutlink(link);
		else
			sender.getImportanceInfo().removeOutlink(link);
	}

	@Override
	public void notifyCredit(final Account account, final Amount amount) {
	}

	@Override
	public void notifyDebit(final Account account, final Amount amount) {
	}
}
