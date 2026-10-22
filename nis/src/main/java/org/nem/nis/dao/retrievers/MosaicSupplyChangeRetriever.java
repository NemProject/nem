package org.nem.nis.dao.retrievers;

import java.util.*;
import java.util.stream.Collectors;
import org.hibernate.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

/**
 * Class for for retrieving mosaic supply change transactions.
 */
public class MosaicSupplyChangeRetriever implements TransactionRetriever {

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(final Session session, final long accountId, final long maxId,
			final int limit, final ReadOnlyTransferDao.TransferType transferType) {
		if (ReadOnlyTransferDao.TransferType.ALL == transferType) {
			throw new IllegalArgumentException("transfer type ALL not supported by transaction retriever classes");
		}

		if (ReadOnlyTransferDao.TransferType.INCOMING == transferType) {
			return Collections.emptyList();
		}

		// JOIN FETCH everything the former Criteria query joined implicitly (the lazy block and every eager to-one); HQL loads each
		// eager association left out here with a separate select per row (N+1)
		final String hql = "SELECT t FROM DbMosaicSupplyChangeTransaction t" // preserve-newline
				+ " LEFT JOIN FETCH t.block b LEFT JOIN FETCH b.harvester LEFT JOIN FETCH b.lessor LEFT JOIN FETCH t.sender" // preserve-newline
				+ " WHERE t.sender.id = :accountId AND t.senderProof IS NOT NULL AND t.id < :maxId" // preserve-newline
				+ " ORDER BY t.sender.id ASC, t.id DESC";
		final List<DbMosaicSupplyChangeTransaction> list = session.createQuery(hql, DbMosaicSupplyChangeTransaction.class)
				.setParameter("accountId", accountId).setParameter("maxId", maxId).setMaxResults(limit).list();
		return list.stream().map(t -> new TransferBlockPair(t, t.getBlock())).collect(Collectors.toList());
	}
}
