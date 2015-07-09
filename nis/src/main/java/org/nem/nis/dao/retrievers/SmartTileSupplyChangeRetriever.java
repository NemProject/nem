package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for for retrieving smart tile supply change transactions.
 */
public class SmartTileSupplyChangeRetriever implements TransactionRetriever {

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

		if (ReadOnlyTransferDao.TransferType.INCOMING == transferType) {
			return Collections.emptyList();
		}

		final Criteria criteria = session.createCriteria(DbSmartTileSupplyChangeTransaction.class)
				.setFetchMode("block", FetchMode.JOIN)
				.setFetchMode("sender", FetchMode.JOIN)
				.add(Restrictions.eq("sender.id", accountId))
				.add(Restrictions.isNotNull("senderProof"))
				.add(Restrictions.lt("id", maxId))
				.addOrder(Order.asc("sender.id"))
				.addOrder(Order.desc("id"))
				.setMaxResults(limit)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		final List<DbSmartTileSupplyChangeTransaction> list = HibernateUtils.listAndCast(criteria);
		return list.stream()
				.map(t -> new TransferBlockPair(t, t.getBlock()))
				.collect(Collectors.toList());
	}
}
