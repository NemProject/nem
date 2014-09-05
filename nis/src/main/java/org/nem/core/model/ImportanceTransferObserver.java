package org.nem.core.model;

public interface ImportanceTransferObserver {

	public void notifyTransfer(final Account sender, final Account recipient, final int direction);
}
