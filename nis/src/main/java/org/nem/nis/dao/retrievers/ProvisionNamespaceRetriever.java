package org.nem.nis.dao.retrievers;

import org.hibernate.Session;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.TransferBlockPair;

import java.util.*;
/**
 * Class for for retrieving provision namespace transactions.
 */
public class ProvisionNamespaceRetriever implements TransactionRetriever {

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		if (ReadOnlyTransferDao.TransferType.ALL == transferType) {
			throw new IllegalArgumentException("transfer type ALL not supported by transaction retriever classes");
		}

		return new ArrayList<>();
	}
}
