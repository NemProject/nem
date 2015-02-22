package org.nem.nis.dao.retrievers;

import org.hibernate.*;
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
			// can't do it with criteria :/
			final List<DbMultisigAggregateModificationTransaction> transactions = getOutgoingDbModificationTransactions(
					session,
					accountId,
					maxId,
					limit);
			return transactions.stream()
					.map(t -> {
						Hibernate.initialize(t.getBlock());
						return new TransferBlockPair(t, t.getBlock());
					})
					.collect(Collectors.toList());
		}

		// can't do it with criteria :/
		final List<DbMultisigAggregateModificationTransaction> transactions = getIncomingDbModificationTransactions(
				session,
				accountId,
				maxId,
				limit);
		return transactions.stream()
				.map(t -> {
					Hibernate.initialize(t.getBlock());
					return new TransferBlockPair(t, t.getBlock());
				})
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<DbMultisigAggregateModificationTransaction> getIncomingDbModificationTransactions(
			final Session session,
			final long cosignatoryId,
			final long maxId,
			final int limit) {
		final String queryString =
				"SELECT mm.multisigsignermodificationid, mm.id as mmId, mm.cosignatoryid, mm.modificationtype, msm.* FROM multisigmodifications mm " +
				"LEFT OUTER JOIN multisigsignermodifications msm on msm.id = mm.multisigsignermodificationid AND msm.senderproof IS NOT NULL " +
				"WHERE mm.multisigsignermodificationid < :maxId AND mm.cosignatoryid = :cosignatoryId AND msm.senderproof IS NOT NULL " +
				"ORDER BY mm.cosignatoryid ASC, mm.multisigsignermodificationid DESC limit :limit";
		final Query query = session
				.createSQLQuery(queryString)
				.addEntity(DbMultisigAggregateModificationTransaction.class)
				.setParameter("maxId", maxId)
				.setParameter("cosignatoryId", cosignatoryId)
				.setParameter("limit", limit);
		return (List<DbMultisigAggregateModificationTransaction>)query.list();
	}

	@SuppressWarnings("unchecked")
	private List<DbMultisigAggregateModificationTransaction> getOutgoingDbModificationTransactions(
			final Session session,
			final long senderId,
			final long maxId,
			final int limit) {
		final String queryString =
				"SELECT msm.* FROM multisigsignermodifications msm " +
				"WHERE msm.id < :maxId AND msm.senderid = :senderId AND msm.senderproof IS NOT NULL " +
				"ORDER BY msm.senderid ASC, msm.id DESC limit :limit";
		final Query query = session
				.createSQLQuery(queryString)
				.addEntity(DbMultisigAggregateModificationTransaction.class)
				.setParameter("maxId", maxId)
				.setParameter("senderId", senderId)
				.setParameter("limit", limit);
		return (List<DbMultisigAggregateModificationTransaction>)query.list();
	}

}
