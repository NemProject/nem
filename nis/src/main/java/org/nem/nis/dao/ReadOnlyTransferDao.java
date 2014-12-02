package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;

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
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransactionsForAccount(final Account account, final Integer timeStamp, final int limit);

	/**
	 * Retrieves limit Transfers from db for given account.
	 * TODO-CR: it might make sense to return a small DTO instead of an Object[]
	 *
	 * @param account The account.
	 * @param hash The hash of "top-most" transfer.
	 * @param height The block height at which to search for the hash.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransactionsForAccountUsingHash(
			final Account account,
			final Hash hash,
			final BlockHeight height,
			final TransferType transferType,
			final int limit);

	/**
	 * Retrieves limit Transfers from db for given account.
	 * TODO-CR: it might make sense to return a small DTO instead of an Object[]
	 * TODO 20141201 J-B: can you do this while you're here?
	 * TODO 20141202 BR -> J: Done. Hope you meant it like that.
	 *
	 * @param account The account.
	 * @param id The id of "top-most" transfer.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransactionsForAccountUsingId(final Account account, final Long id, final TransferType transferType, final int limit);
}
