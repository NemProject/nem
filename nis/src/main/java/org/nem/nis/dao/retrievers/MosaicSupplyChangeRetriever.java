package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

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

		final Criteria criteria = session.createCriteria(DbMosaicSupplyChangeTransaction.class) // preserve-newline
				.setFetchMode("block", FetchMode.JOIN) // preserve-newline
				.setFetchMode("sender", FetchMode.JOIN) // preserve-newline
				.add(Restrictions.eq("sender.id", accountId)) // preserve-newline
				.add(Restrictions.isNotNull("senderProof")) // preserve-newline
				.add(Restrictions.lt("id", maxId)) // preserve-newline
				.addOrder(Order.asc("sender.id")) // preserve-newline
				.addOrder(Order.desc("id")) // preserve-newline
				.setMaxResults(limit) // preserve-newline
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		final List<DbMosaicSupplyChangeTransaction> list = HibernateUtils.listAndCast(criteria);
		return list.stream().map(t -> new TransferBlockPair(t, t.getBlock())).collect(Collectors.toList());
	}
}
