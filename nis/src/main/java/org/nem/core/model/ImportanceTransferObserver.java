package org.nem.core.model;

// TODO 20140909 J-G: comment
// TODO 20140909 J-G: does this need to be in core or can it be moved to secret with most of our other observers?

public interface ImportanceTransferObserver {

	public void notifyTransfer(final Account sender, final Account recipient, final int direction);
}
