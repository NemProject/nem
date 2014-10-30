package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.Transfer;

import java.util.Collection;

/**
 * Read-only DAO for accessing db Transfer objects.
 */
public interface ReadOnlyTransferDao {
	/*
	 * Types of transfers that can be requested.
	 */
	public enum TransferType {
		ALL,
		INCOMING,
		OUTGOING
	}

	/**
	 * Returns number of transfers in the database.
	 * <p>
	 * Note: this  will return number of transactions of Transfer type only.
	 *
	 * @return number of transfers in the database.
	 */
	public Long count();

	/**
	 * Retrieves Transfer from db given it's hash.
	 *
	 * @param txHash hash of a transfer to retrieve.
	 * @return Transfer having given hash or null.
	 */
	public Transfer findByHash(byte[] txHash);

	/**
	 * Retrieves Transfer from db given it's hash. The search is only up to a given block height.
	 *
	 * @param txHash hash of a transfer to retrieve.
	 * @param maxBlockHeight The maximum block height.
	 * @return Transfer having given hash or null.
	 */
	public Transfer findByHash(byte[] txHash, long maxBlockHeight);

	/**
	 * Gets a value indicating whether or not any of the given hashes exist in the db.
	 * The search is only up to a given block height.
	 *
	 * @param hashes A collections of hashes.
	 * @param maxBlockHeight The maximum block height.
	 * @return True if any of the given hashes already exist in the db, false otherwise.
	 */
	public boolean duplicateHashExists(Collection<Hash> hashes, BlockHeight maxBlockHeight);

	/**
	 * Retrieves limit Transfers from db for given account.
	 *
	 * @param account The account.
	 * @param timeStamp The maximum timestamp of a transfer.
	 * @param limit The limit.
	 * @return Collection of Transfer information.
	 * Each Object array will contain two elements:
	 * { [0] - Transfer (Transfer), [1] Block Height (long) }
	 */
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timeStamp, final int limit);

	/**
	 * Retrieves limit Transfers from db for given account.
	 * TODO-CR: it might make sense to return a small DTO instead of an Object[]
	 *
	 * @param account The account.
	 * @param hash The hash of "top-most" transfer.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of Transfer information.
	 * Each Object array will contain two elements:
	 * { [0] - Transfer (Transfer), [1] Block Height (long) }
	 */
	public Collection<Object[]> getTransactionsForAccountUsingHash(final Account account, final Hash hash, final TransferType transferType, final int limit);
}
