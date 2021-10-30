package org.nem.nis.dao.retrievers;

import org.hibernate.Session;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.TransferBlockPair;

import java.util.Collection;

/**
 * Interface for retrieving transfers of a specific type.
 */
public interface TransactionRetriever {

	/**
	 * Retrieves limit transfers from db for given account.
	 *
	 * @param session The current database session,
	 * @param accountId The account id.
	 * @param maxId The id of "top-most" transfer.
	 * @param limit The limit.
	 * @param transferType Type of returned transfers.
	 * @return Collection of transfer block pairs.
	 */
	Collection<TransferBlockPair> getTransfersForAccount(final Session session, final long accountId, final long maxId, final int limit,
			final ReadOnlyTransferDao.TransferType transferType);
}
