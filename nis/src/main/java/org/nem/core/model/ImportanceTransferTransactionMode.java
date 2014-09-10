package org.nem.core.model;

/**
 * Static class containing modes of ImportanceTransferTransaction.
 */
public class ImportanceTransferTransactionMode {
	/**
	 * When announcing importance transfer.
	 */
	public static final int Activate = 1;

	/**
	 * When canceling association between account and importance transfer.
	 */
	public static final int Deactivate = 2;
}
