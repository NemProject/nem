package org.nem.nis.dao;

import java.util.List;

import org.nem.nis.dbmodel.Transfer;

/**
 * DAO for accessing db Transfer objects (transaction transfer)
 */
public interface TransferDao extends ReadOnlyTransferDao {
	/**
	 * Saves transfer in the database
	 *
	 * @param transfer Transfer that's going to be saved.
	 */
	public void save(Transfer transfer);

	/**
	 * Save multiple transfers at once (in a single transaction).
	 *
	 * @param transfers list of transfers to be saved.
	 */
	public void saveMulti(List<Transfer> transfers);
}