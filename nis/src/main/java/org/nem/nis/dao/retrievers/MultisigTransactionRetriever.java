package org.nem.nis.dao.retrievers;

import java.util.*;
import java.util.stream.*;
import org.hibernate.*;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.Query;
import org.hibernate.type.LongType;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;

/**
 * Class for for retrieving multisig transactions.
 */
@SuppressWarnings("rawtypes")
public class MultisigTransactionRetriever implements TransactionRetriever {
	private static final Map<Integer, String> TYPE_TO_FIELD_NAME_MAP = TransactionRegistry.stream().filter(e -> null != e.multisigJoinField)
			.collect(Collectors.toMap(e -> e.type, e -> e.multisigJoinField));

	@Override
	public Collection<TransferBlockPair> getTransfersForAccount(final Session session, final long accountId, final long maxId,
			final int limit, final ReadOnlyTransferDao.TransferType transferType) {
		if (ReadOnlyTransferDao.TransferType.ALL == transferType) {
			throw new IllegalArgumentException("transfer type ALL not supported by transaction retriever classes");
		}

		final Collection<TransferBlockPair> pairs = new ArrayList<>();
		for (final Map.Entry<Integer, String> entry : TYPE_TO_FIELD_NAME_MAP.entrySet()) {
			pairs.addAll(this.getMultisigTransactionsForAccount(session, accountId, maxId, limit, transferType, entry.getKey(),
					entry.getValue()));
		}
		return this.sortAndLimit(pairs, limit);
	}

	private Collection<TransferBlockPair> getMultisigTransactionsForAccount(final Session session, final long accountId, final long maxId,
			final int limit, final ReadOnlyTransferDao.TransferType transferType, final int transactionType, final String joinField) {
		final List<TransactionIdBlockHeightPair> listOfIds = this.getMultisigIds(session, transferType, accountId, transactionType, maxId,
				limit);
		if (listOfIds.isEmpty()) {
			return new ArrayList<>();
		}

		final List<DbMultisigTransaction> transactions = this.getMultisigTransactions(session, listOfIds, joinField);
		final HashMap<Long, DbBlock> blockMap = this.getBlockMap(session, listOfIds);
		return IntStream.range(0, transactions.size())
				.mapToObj(i -> new TransferBlockPair(transactions.get(i), blockMap.get(listOfIds.get(i).blockHeight)))
				.collect(Collectors.toList());
	}

	private List<TransactionIdBlockHeightPair> getMultisigIds(final Session session, final ReadOnlyTransferDao.TransferType transferType,
			final long accountId, final int type, final long maxId, final int limit) {
		final String table = ReadOnlyTransferDao.TransferType.OUTGOING.equals(transferType) ? "multisigsends" : "multisigreceives";
		final String preQueryTemplate = "SELECT transactionId, height FROM " + table
				+ " WHERE accountId=%d AND type=%d AND transactionId < %d " // preserve-newline
				+ "ORDER BY accountId asc, type asc, transactionId DESC";
		final String preQueryString = String.format(preQueryTemplate, accountId, type, maxId);
		final Query<?> preQuery = session.createNativeQuery(preQueryString) // preserve-newline
				.addScalar("transactionId", LongType.INSTANCE) // preserve-newline
				.addScalar("height", LongType.INSTANCE) // preserve-newline
				.setMaxResults(limit);
		final List<Object[]> list = HibernateUtils.listAndCast(preQuery);
		return list.stream().map(o -> new TransactionIdBlockHeightPair((Long) o[0], (Long) o[1])).collect(Collectors.toList());
	}

	private List<DbMultisigTransaction> getMultisigTransactions(final Session session, final List<TransactionIdBlockHeightPair> pairs,
			final String joinEntity) {
		// JOIN FETCH every eager association the former Criteria query joined implicitly (the sender, the wrapped transaction and its
		// eager associations, see getInnerTransactionFetches); HQL loads each one left out here with a separate select per row (N+1)
		// joinEntity is one of the TYPE_TO_FIELD_NAME_MAP code constants, never user input;
		// the inner join also excludes multisig transactions wrapping a different transaction type
		final List<Long> ids = pairs.stream().map(p -> p.transactionId).collect(Collectors.toList());
		// DISTINCT (in memory only, not passed to SQL) collapses the duplicate root rows produced by fetching the eager
		// multisigModifications collection; it keeps the first occurrence, so the id-descending order is preserved
		final String hql = "SELECT DISTINCT m FROM DbMultisigTransaction m LEFT JOIN FETCH m.sender" // preserve-newline
				+ " JOIN FETCH m." + joinEntity + " t LEFT JOIN FETCH t.sender" // preserve-newline
				+ getInnerTransactionFetches(joinEntity) // preserve-newline
				+ " WHERE m.id IN (:ids) ORDER BY m.id DESC";
		final List<DbMultisigTransaction> result = session.createQuery(hql, DbMultisigTransaction.class).setParameterList("ids", ids)
				.setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false).list();
		// we deliberately set multisigSignatureTransactions to lazy, to avoid
		// duplicates inside other entities within current "joinEntity"
		// (i.e. TransferTransaction holds mosaics, so each mosaic would get duplicated
		// for every cosignatory)
		// now we need to force loading of child entities
		for (final DbMultisigTransaction transaction : result) {
			transaction.getMultisigSignatureTransactions().size();
		}
		return result;
	}

	private static String getInnerTransactionFetches(final String joinEntity) {
		// fetch the eager associations of the inner transaction type, so they are not loaded by a select per row
		switch (joinEntity) {
			case "transferTransaction":
				return " LEFT JOIN FETCH t.recipient";
			case "importanceTransferTransaction":
				return " LEFT JOIN FETCH t.remote";
			case "multisigAggregateModificationTransaction":
				return " LEFT JOIN FETCH t.multisigMinCosignatoriesModification"
						+ " LEFT JOIN FETCH t.multisigModifications mm LEFT JOIN FETCH mm.cosignatory";
			case "provisionNamespaceTransaction":
				return " LEFT JOIN FETCH t.rentalFeeSink LEFT JOIN FETCH t.namespace ns LEFT JOIN FETCH ns.owner";
			case "mosaicDefinitionCreationTransaction":
				return " LEFT JOIN FETCH t.creationFeeSink LEFT JOIN FETCH t.mosaicDefinition md"
						+ " LEFT JOIN FETCH md.creator LEFT JOIN FETCH md.feeRecipient";
			default :
				return "";
		}
	}

	private HashMap<Long, DbBlock> getBlockMap(final Session session, final List<TransactionIdBlockHeightPair> pairs) {
		final HashMap<Long, DbBlock> blockMap = new HashMap<>();
		// b.harvester and b.lessor are eager; without the JOIN FETCH, HQL would load each with a separate select per row (N+1)
		final List<DbBlock> blocks = session
				.createQuery("SELECT b FROM DbBlock b LEFT JOIN FETCH b.harvester LEFT JOIN FETCH b.lessor"
						+ " WHERE b.height IN (:heights) ORDER BY b.height DESC", DbBlock.class)
				.setParameterList("heights", pairs.stream().map(p -> p.blockHeight).collect(Collectors.toList())).list();
		blocks.stream().forEach(b -> blockMap.put(b.getHeight(), b));
		return blockMap;
	}

	private Collection<TransferBlockPair> sortAndLimit(final Collection<TransferBlockPair> pairs, final int limit) {
		final List<TransferBlockPair> list = pairs.stream().sorted().collect(Collectors.toList());
		TransferBlockPair curPair = null;
		final Collection<TransferBlockPair> result = new ArrayList<>();
		for (final TransferBlockPair pair : list) {
			if (null == curPair || !(curPair.getTransfer().getId().equals(pair.getTransfer().getId()))) {
				result.add(pair);
				if (limit == result.size()) {
					break;
				}
			}
			curPair = pair;
		}

		return result;
	}

	private class TransactionIdBlockHeightPair {
		private final Long transactionId;
		private final Long blockHeight;

		private TransactionIdBlockHeightPair(final Long transactionId, final Long blockHeight) {
			this.transactionId = transactionId;
			this.blockHeight = blockHeight;
		}
	}
}
