package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.nis.dbmodel.Transfer;

import java.util.Collection;

/**
 * Read-only DAO for accessing db Transfer objects.
 */
public interface ReadOnlyTransferDao extends SimpleReadOnlyTransferDao<Transfer> {
	/*
	 * Types of transfers that can be requested.
	 */
	public enum TransferType {
		ALL,
		INCOMING,
		OUTGOING
	}

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
