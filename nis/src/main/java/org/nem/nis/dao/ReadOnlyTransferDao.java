package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
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
	 * <p/>
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
	 * Retrieves limit Transfers from db for given account.
	 *
	 * @param account The account.
	 * @param timestamp The maximum timestamp of a transfer.
	 * @param limit The limit.
	 * @return Collection of Transfer information.
	 * Each Object array will contain two elements:
	 * { [0] - Transfer (Transfer), [1] Block Height (long) }
	 */
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timestamp, final int limit);

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
