package org.nem.nis.dao;

import java.util.List;

/**
 * Read-write DAO for accessing db transaction objects.
 *
 * @param <TTransfer> The transfer type.
 */
public interface SimpleTransferDao<TTransfer> {

	/**
	 * Saves or updates entity in the database.
	 *
	 * @param entity Importance transfer that's going to be saved.
	 */
	void save(final TTransfer entity);

	/**
	 * Saves multiple entities at once (in a single transaction).
	 *
	 * @param transfers list of transfers to be saved.
	 */
	void saveMulti(List<TTransfer> transfers);
}