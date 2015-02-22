package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.nis.dao.ReadOnlyTransferDao;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for for retrieving multisig modification transactions.
 */
public class MultisigModificationRetriever implements TransactionRetriever {

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(
			final Session session,
			final long accountId,
			final long maxId,
			final int limit,
			final ReadOnlyTransferDao.TransferType transferType) {
		if (ReadOnlyTransferDao.TransferType.OUTGOING == transferType) {
			final Criteria criteria = session.createCriteria(DbMultisigAggregateModificationTransaction.class)
					.setFetchMode("blockId", FetchMode.JOIN)
					.setFetchMode("sender", FetchMode.JOIN)
					.add(Restrictions.eq("sender.id", accountId))
					.add(Restrictions.isNotNull("senderProof"))
					.add(Restrictions.lt("id", maxId))
					.addOrder(Order.asc("sender"))
					.addOrder(Order.desc("id"))
					.setMaxResults(limit);
			criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			final List<DbMultisigAggregateModificationTransaction> list = criteria.list();
			return list.stream()
					.map(t -> {
						// force lazy initialization
						Hibernate.initialize(t.getBlock());
						return new TransferBlockPair(t, t.getBlock());
					})
					.collect(Collectors.toList());
		}

		final Criteria criteria = session.createCriteria(DbMultisigModification.class)
				.setFetchMode("multisigAggregateModificationTransaction", FetchMode.JOIN)
				.setFetchMode("cosignatory", FetchMode.JOIN)
				.add(Restrictions.eq("cosignatory.id", accountId))
				.add(Restrictions.lt("multisigAggregateModificationTransaction.id", maxId))
				.addOrder(Order.asc("cosignatory"))
				.addOrder(Order.desc("multisigAggregateModificationTransaction.id"))
				.setMaxResults(limit);

		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		final List<DbMultisigModification> list = criteria.list();
		return list.stream()
				.map(m -> {
					Hibernate.initialize(m.getMultisigAggregateModificationTransaction());
					Hibernate.initialize(m.getMultisigAggregateModificationTransaction().getBlock());
					return new TransferBlockPair(m.getMultisigAggregateModificationTransaction(), m.getMultisigAggregateModificationTransaction().getBlock());
				})
				.collect(Collectors.toList());
	}
}
