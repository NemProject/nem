package org.nem.nis.dao;

import org.nem.nis.dbmodel.ImportanceTransfer;

// TODO 20140909 J-G: not sure if this buys us anything, but we could have a generic TransferDao interface that we could use for all transaction types

public interface ImportanceTransferDao {
	/**
	 * Save or update entity in the database.
	 *
	 * @param entity Importance transfer that's going to be saved.
	 */
	void save(final ImportanceTransfer entity);

	/**
	 * Returns number of importance transfers in the database.
	 *
	 * @return number of importance transfers in the database.
	 */
	Long count();

	/**
	 * Retrieves ImportanceTransfer from db given it's hash.
	 *
	 * @param txHash Hash of a transfer to retrieve.
	 * @return Transfer having given hash or null.
	 */
	ImportanceTransfer findByHash(final byte[] txHash);
}
