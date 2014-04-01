package org.nem.core.dao;

import java.util.List;

import org.nem.core.dbmodel.Transfer;

/**
 * DAO for accessing db Transfer objects (transaction transfer)
 */
public interface TransferDao {
	/**
	 * Saves transfer in the database
	 *
	 * @param transfer Transfer that's going to be saved.
	 */
	public void save(Transfer transfer);

	/**
	 * Returns number of transfers in the database.
	 * <p/>
	 * Note: this  will return number of transactions of Transfer type only.
	 *
	 * @return number of transfers in the database.
	 */
	public Long count();

	/**
	 * Save multiple transfers at once (in a single transaction).
	 *
	 * @param transfers list of transfers to be saved.
	 */
	public void saveMulti(List<Transfer> transfers);

	/**
	 * Retrieves Transfer from db given it's hash.
	 *
	 * @param txHash hash of a transfer to retrieve.
	 *
	 * @return Transfer having given hash or null.
	 */
	public Transfer findByHash(byte[] txHash);
}