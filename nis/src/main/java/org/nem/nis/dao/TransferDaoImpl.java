package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TransferDaoImpl implements TransferDao {
	private final SimpleTransferDaoImpl<Transfer> impl;

	@Autowired(required = true)
	public TransferDaoImpl(final SessionFactory sessionFactory) {
		this.impl = new SimpleTransferDaoImpl<>("Transfer", sessionFactory, Transfer::getTransferHash);
	}

	private Session getCurrentSession() {
		return this.impl.getCurrentSession();
	}

	//region SimpleTransferDao

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return this.impl.count();
	}

	@Override
	@Transactional(readOnly = true)
	public Transfer findByHash(final byte[] txHash) {
		return this.impl.findByHash(txHash);
	}

	@Override
	@Transactional(readOnly = true)
	public Transfer findByHash(final byte[] txHash, final long maxBlockHeight) {
		return this.impl.findByHash(txHash, maxBlockHeight);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean anyHashExists(final Collection<Hash> hashes, final BlockHeight maxBlockHeight) {
		return this.impl.anyHashExists(hashes, maxBlockHeight);
	}

	@Override
	@Transactional
	public void save(final Transfer entity) {
		this.impl.save(entity);
	}

	@Override
	@Transactional
	public void saveMulti(final List<Transfer> transfers) {
		this.impl.saveMulti(transfers);
	}

	//endregion

	// NOTE: this query will also ask for accounts of senders and recipients!
	@Override
	@Transactional(readOnly = true)
	public Collection<Object[]> getTransactionsForAccount(final Account address, final Integer timeStamp, final int limit) {
		// TODO: have no idea how to do it using Criteria...
		final Query query = this.getCurrentSession()
				.createQuery("select t, t.block.height from Transfer t " +
						"where t.timeStamp <= :timeStamp AND (t.recipient.printableKey = :pubkey OR t.sender.printableKey = :pubkey) " +
						"order by t.timeStamp desc")
				.setParameter("timeStamp", timeStamp)
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		return listAndCast(query);
	}

	private String buildAddressQuery(final TransferType transferType) {
		switch (transferType) {
			case INCOMING:
				return "(t.recipient.printableKey = :pubkey)";
			case OUTGOING:
				return "(t.sender.printableKey = :pubkey)";
		}
		return "(t.recipient.printableKey = :pubkey OR t.sender.printableKey = :pubkey)";
	}

	@Override
	@Transactional
	public Collection<Object[]> getTransactionsForAccountUsingHash(final Account address, final Hash hash, final TransferType transferType, final int limit) {
		final String addressString = this.buildAddressQuery(transferType);
		if (hash == null) {
			return this.getLatestTransactionsForAccount(address, limit, transferType);
		} else {
			final Object[] tx = this.getTransactionDescriptorUsingHash(address, hash, limit, addressString);
			return this.getTransactionsForAccountUpToTransaction(address, limit, transferType, tx);
		}
	}

	private Object[] getTransactionDescriptorUsingHash(final Account address, final Hash hash, final int limit, final String addressString) {
		final Query prequery = this.getCurrentSession()
				.createQuery("select t, t.block.height from Transfer t " +
						"WHERE " +
						addressString +
						" AND t.transferHash = :hash" +
						" ORDER BY t.timeStamp desc")
				.setParameter("hash", hash.getRaw())
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		final List<Object[]> tempList = listAndCast(prequery);
		if (tempList.size() < 1) {
			throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
		}

		return tempList.get(0);
	}

	private Collection<Object[]> getTransactionsForAccountUpToTransaction(
			final Account address,
			final int limit,
			final TransferType transferType,
			final Object[] tx) {
		if (TransferType.ALL == transferType) {
			final Collection<Object[]> objects = this.getTransactionsForAccountUpToTransactionWithTransferType(address, limit, TransferType.INCOMING, tx);
			objects.addAll(this.getTransactionsForAccountUpToTransactionWithTransferType(address, limit, TransferType.OUTGOING, tx));
			return sortAndLimit(objects, limit);
		} else {
			return this.getTransactionsForAccountUpToTransactionWithTransferType(address, limit, transferType, tx);
		}
	}

	private Collection<Object[]> getTransactionsForAccountUpToTransactionWithTransferType(
			final Account address,
			final int limit,
			final TransferType transferType,
			final Object[] tx) {
		final Query query;
		final Transfer topMostTransfer = (Transfer)tx[0];

		final long blockHeight = (long)tx[1];
		final int timeStamp = topMostTransfer.getTimeStamp();
		final int indexInsideBlock = topMostTransfer.getBlkIndex();

		query = this.getCurrentSession()
				.createQuery("select t, t.block.height from Transfer t " +
						"WHERE " +
						this.buildAddressQuery(transferType) +
						" AND ((t.block.height < :height)" +
						" OR (t.block.height = :height AND t.timeStamp < :timeStamp)" +
						" OR (t.block.height = :height AND t.timeStamp = :timeStamp AND t.blkIndex > :blockIndex))" +
						" ORDER BY t.block.height DESC, t.timeStamp DESC, t.blkIndex ASC")
				.setParameter("height", blockHeight)
				.setParameter("timeStamp", timeStamp)
				.setParameter("blockIndex", indexInsideBlock)
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		return listAndCast(query);
	}

	private Collection<Object[]> getLatestTransactionsForAccount(
			final Account address,
			final int limit,
			final TransferType transferType) {
		if (TransferType.ALL == transferType) {
			final Collection<Object[]> objects = this.getLatestTransactionsForAccountWithTransferType(address, limit, TransferType.INCOMING);
			objects.addAll(this.getLatestTransactionsForAccountWithTransferType(address, limit, TransferType.OUTGOING));
			return sortAndLimit(objects, limit);
		} else {
			return this.getLatestTransactionsForAccountWithTransferType(address, limit, transferType);
		}
	}

	private Collection<Object[]> getLatestTransactionsForAccountWithTransferType(
			final Account address,
			final int limit,
			final TransferType transferType) {
		final Query query = this.getCurrentSession()
				.createQuery("select t, t.block.height from Transfer t " +
						"WHERE " +
						this.buildAddressQuery(transferType) +
						" ORDER BY t.block.height DESC, t.timeStamp DESC, t.blkIndex ASC, t.transferHash ASC")
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		return listAndCast(query);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Query q) {
		return q.list();
	}

	private Collection<Object[]> sortAndLimit(final Collection<Object[]> objects, final int limit) {
		return objects.stream()
				.sorted((o1, o2) -> comparePair(o1, o2))
				.limit(limit)
				.collect(Collectors.toList());
	}

	private int comparePair(final Object[] lhs, final Object[] rhs) {
		final Transfer lhsTransfer = (Transfer)lhs[0];
		final Long lhsHeight = (long)lhs[1];
		final Transfer rhsTransfer = (Transfer)rhs[0];
		final Long rhsHeight = (long)rhs[1];

		final int heightComparison = -lhsHeight.compareTo(rhsHeight);
		if (0 != heightComparison) {
			return heightComparison;
		}

		final int timeStampComparison = -lhsTransfer.getTimeStamp().compareTo(rhsTransfer.getTimeStamp());
		if (0 != timeStampComparison) {
			return timeStampComparison;
		}

		final int blockIndexComparison = lhsTransfer.getBlkIndex().compareTo(rhsTransfer.getBlkIndex());
		if (0 != blockIndexComparison) {
			return blockIndexComparison;
		}

		final int hashComparison = lhsTransfer.getTransferHash().toString().compareTo(rhsTransfer.getTransferHash().toString());
		if (0 != hashComparison) {
			return hashComparison;
		}

		return 0;
	}
}
