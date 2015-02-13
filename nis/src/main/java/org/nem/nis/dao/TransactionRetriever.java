package org.nem.nis.dao;

import org.hibernate.Session;
import org.nem.nis.dbmodel.TransferBlockPair;

import java.util.Collection;

/**
 * Interface for retrieving transfers of a specific type.
 * TODO 20150212 J-B: not sure if this is a good idea ... trying to test the TransferDao as-is was pretty difficult
 * > due to all the reasons you mentioned in IRC (and self-delegation tests are usually not a good idea)
 * > my natural inclination was to break things up more so that they're more easily testable
 * > so, this is what i came up with
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
	public Collection<TransferBlockPair> getTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType);
}
