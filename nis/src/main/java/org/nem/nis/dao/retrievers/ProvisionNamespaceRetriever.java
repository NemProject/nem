package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.stream.Collectors;

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

		// TODO 20150619 J-B: so, the reason incoming == {} is because outgoing is a transaction initiated by the current account (a namespace provisioning),
		// > so, incoming is meaningless?
		// TODO 20150620 BR -> J: Now that we have a lessor, the transaction should be displayed as incoming for the lessor.
		final String senderOrLessor = ReadOnlyTransferDao.TransferType.OUTGOING.equals(transferType) ? "sender" : "lessor";
		final Criteria criteria = session.createCriteria(DbProvisionNamespaceTransaction.class)
				.setFetchMode("block", FetchMode.JOIN)
				.setFetchMode("sender", FetchMode.JOIN)
				.setFetchMode("lessor", FetchMode.JOIN)
				.setFetchMode("namespace", FetchMode.JOIN)
				.add(Restrictions.eq(senderOrLessor + ".id", accountId))
				.add(Restrictions.isNotNull("senderProof"))
				.add(Restrictions.lt("id", maxId))
				.addOrder(Order.asc(senderOrLessor))
				.addOrder(Order.desc("id"))
				.setMaxResults(limit);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		final List<DbProvisionNamespaceTransaction> list = HibernateUtils.listAndCast(criteria);
		return list.stream()
				.map(t -> new TransferBlockPair(t, t.getBlock()))
				.collect(Collectors.toList());
	}
}
