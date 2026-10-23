package org.nem.nis.dao.retrievers;

import java.util.*;
import java.util.stream.Collectors;
import org.hibernate.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

/**
 * Class for for retrieving provision namespace transactions.
 */
public class ProvisionNamespaceRetriever implements TransactionRetriever {

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(final Session session, final long accountId, final long maxId,
			final int limit, final ReadOnlyTransferDao.TransferType transferType) {
		if (ReadOnlyTransferDao.TransferType.ALL == transferType) {
			throw new IllegalArgumentException("transfer type ALL not supported by transaction retriever classes");
		}

		final String senderOrRentalFeeSink = ReadOnlyTransferDao.TransferType.OUTGOING.equals(transferType) ? "sender" : "rentalFeeSink";
		// JOIN FETCH everything the former Criteria query joined implicitly (the lazy block and every eager to-one); HQL loads each
		// eager association left out here with a separate select per row (N+1)
		// senderOrRentalFeeSink is one of two code constants, never user input
		final String hql = "SELECT t FROM DbProvisionNamespaceTransaction t" // preserve-newline
				+ " LEFT JOIN FETCH t.block b LEFT JOIN FETCH b.harvester LEFT JOIN FETCH b.lessor" // preserve-newline
				+ " LEFT JOIN FETCH t.sender LEFT JOIN FETCH t.rentalFeeSink" // preserve-newline
				+ " LEFT JOIN FETCH t.namespace ns LEFT JOIN FETCH ns.owner" // preserve-newline
				+ " WHERE t." + senderOrRentalFeeSink + ".id = :accountId AND t.senderProof IS NOT NULL AND t.id < :maxId" // preserve-newline
				+ " ORDER BY t." + senderOrRentalFeeSink + ".id ASC, t.id DESC";
		final List<DbProvisionNamespaceTransaction> list = session.createQuery(hql, DbProvisionNamespaceTransaction.class)
				.setParameter("accountId", accountId).setParameter("maxId", maxId).setMaxResults(limit).list();
		return list.stream().map(t -> new TransferBlockPair(t, t.getBlock())).collect(Collectors.toList());
	}
}
