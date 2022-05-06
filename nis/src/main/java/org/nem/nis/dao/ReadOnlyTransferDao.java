package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.TransferBlockPair;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbTransferTransaction objects.
 */
public interface ReadOnlyTransferDao {
	/*
	 * Types of transfers that can be requested.
	 */
	enum TransferType {
		ALL, INCOMING, OUTGOING
	}

	/**
	 * Retrieves a transfer with specified hash from the db together with the corresponding block.
	 *
	 * @param hash The hash of the transfer.
	 * @param height The block height at which to search for the hash.
	 * @return transfer block pair.
	 */
	TransferBlockPair getTransactionUsingHash(final Hash hash, final BlockHeight height);

	/**
	 * Retrieves limit Transfers from db for given account.
	 *
	 * @param account The account.
	 * @param hash The hash of "top-most" transfer.
	 * @param height The block height at which to search for the hash.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	Collection<TransferBlockPair> getTransactionsForAccountUsingHash(final Account account, final Hash hash, final BlockHeight height,
			final TransferType transferType, final int limit);

	/**
	 * Retrieves limit transfers from db for given account. These transfers can by of any type.
	 *
	 * @param account The account.
	 * @param id The id of "top-most" transfer.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	Collection<TransferBlockPair> getTransactionsForAccountUsingId(final Account account, final Long id, final TransferType transferType,
			final int limit);
}
