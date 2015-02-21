package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for for retrieving transfer transactions.
 */
public class TransferRetriever implements TransactionRetriever {

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		// TODO 20150111 J-G: should probably add test with senderProof NULL to test that it's being filtered (here and one other place too)
		final String senderOrRecipient = ReadOnlyTransferDao.TransferType.OUTGOING.equals(transferType) ? "sender" : "recipient";
		final Criteria criteria = session.createCriteria(DbTransferTransaction.class)
				.setFetchMode("blockId", FetchMode.JOIN)
				.setFetchMode("sender", FetchMode.JOIN)
				.setFetchMode("recipient", FetchMode.JOIN)
				.add(Restrictions.eq(senderOrRecipient + ".id", accountId))
				.add(Restrictions.isNotNull("senderProof"))
				.add(Restrictions.lt("id", maxId))
				.addOrder(Order.asc(senderOrRecipient))
				.addOrder(Order.desc("id"))
				.setMaxResults(limit);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		final List<DbTransferTransaction> list = criteria.list();
		return list.stream()
				.map(t -> {
					// force lazy initialization
					Hibernate.initialize(t.getBlock());
					return new TransferBlockPair(t, t.getBlock());
				})
				.collect(Collectors.toList());
	}
}
