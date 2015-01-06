package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbTransferTransaction objects.
 *
 * @param <TTransfer> The transfer type.
 */
public interface SimpleReadOnlyTransferDao<TTransfer> {

	/**
	 * Returns number of transfers in the database.
	 *
	 * @return number of transfers in the database.
	 */
	Long count();

	/**
	 * Retrieves a transfer from the db given its hash.
	 *
	 * @param txHash Hash of the transfer to retrieve.
	 * @return DbTransferTransaction having given hash or null.
	 */
	TTransfer findByHash(final byte[] txHash);

	/**
	 * Retrieves a transfer from db given its hash. The search is only up to a given block height
	 *
	 * @param txHash hash of a transfer to retrieve.
	 * @param maxBlockHeight The maximum block height.
	 * @return DbTransferTransaction having given hash or null.
	 */
	TTransfer findByHash(byte[] txHash, long maxBlockHeight);

	/**
	 * Gets a value indicating whether or not any of the given hashes exist in the db.
	 * The search is only up to a given block height.
	 *
	 * @param hashes A collections of hashes.
	 * @param maxBlockHeight The maximum block height.
	 * @return True if any of the given hashes already exist in the db, false otherwise.
	 */
	public boolean anyHashExists(Collection<Hash> hashes, BlockHeight maxBlockHeight);
}
