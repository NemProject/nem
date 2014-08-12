package org.nem.nis.dao;

import java.util.*;
import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TransferDaoImpl implements TransferDao {

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public TransferDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional
	public void save(final Transfer transaction) {
		this.getCurrentSession().saveOrUpdate(transaction);
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)this.getCurrentSession().createQuery("select count (*) from Transfer").uniqueResult();
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional(readOnly = true)
	public Transfer findByHash(final byte[] txHash) {
		final long txId = ByteUtils.bytesToLong(txHash);
		final List<?> userList;
		final Query query = this.getCurrentSession()
			.createQuery("from Transfer a where a.shortId = :id")
			.setParameter("id", txId);
		userList = query.list();
		for (final Object transferObject : userList) {
			final Transfer transfer = (Transfer)transferObject;
			if (Arrays.equals(txHash, transfer.getTransferHash().getRaw())) {
				return transfer;
			}
		}
		return null;
	}

	// NOTE: this query will also ask for accounts of senders and recipients!
	@Override
	@Transactional(readOnly = true)
	public Collection<Object[]> getTransactionsForAccount(final Account address, final Integer timestamp, final int limit) {
		// TODO: have no idea how to do it using Criteria...
		final Query query = this.getCurrentSession()
			.createQuery("select t, t.block.height from Transfer t " +
					"where t.timestamp <= :timestamp AND (t.recipient.printableKey = :pubkey OR t.sender.printableKey = :pubkey) " +
					"order by t.timestamp desc")
			.setParameter("timestamp", timestamp)
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
			return this.getLatestTransactionsForAccount(address, limit, addressString);
		} else {
			final Object[] tx = this.getTransactionDescriptorUsingHash(address, hash, limit, addressString);
			return this.getTransactionsForAccountUptoTransaction(address, limit, addressString, tx);
		}
	}

	private Object[] getTransactionDescriptorUsingHash(final Account address, final Hash hash, final int limit, final String addressString) {
		final Query prequery = this.getCurrentSession()
			.createQuery("select t, t.block.height from Transfer t " +
					"WHERE " +
					addressString +
					" AND t.transferHash = :hash" +
					" ORDER BY t.timestamp desc")
			.setParameter("hash", hash.getRaw())
			.setParameter("pubkey", address.getAddress().getEncoded())
			.setMaxResults(limit);
		final List<Object[]> tempList = listAndCast(prequery);
		if (tempList.size() < 1) {
			throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
		}

		return tempList.get(0);
	}

	private Collection<Object[]> getTransactionsForAccountUptoTransaction(final Account address, final int limit, final String addressString, final Object[] tx) {
		final Query query;
		final Transfer topMostTranser = (Transfer)tx[0];

		final long blockHeight = (long)tx[1];
		final int timestamp = topMostTranser.getTimestamp();
		final int indexInsideBlock = topMostTranser.getBlkIndex();

		query = this.getCurrentSession()
				.createQuery("select t, t.block.height from Transfer t " +
						"WHERE " +
						addressString +
						" AND ((t.block.height < :height)" +
						" OR (t.block.height = :height AND t.timestamp < :timestamp)" +
						" OR (t.block.height = :height AND t.timestamp = :timestamp AND t.blkIndex > :blockIndex))" +
						" ORDER BY t.block.height DESC, t.timestamp DESC, t.blkIndex ASC")
				.setParameter("height", blockHeight)
				.setParameter("timestamp", timestamp)
				.setParameter("blockIndex", indexInsideBlock)
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		return listAndCast(query);
	}

	private Collection<Object[]> getLatestTransactionsForAccount(final Account address, final int limit, final String addressString) {
		final Query query = this.getCurrentSession()
			.createQuery("select t, t.block.height from Transfer t " +
					"WHERE " +
					addressString +
					" ORDER BY t.block.height DESC, t.timestamp DESC, t.blkIndex ASC, t.transferHash ASC")
			.setParameter("pubkey", address.getAddress().getEncoded())
			.setMaxResults(limit);

		return listAndCast(query);
	}

	@Override
	public void saveMulti(final List<Transfer> transfers) {
		final Session sess = this.sessionFactory.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = sess.beginTransaction();
			int i = 0;
			for (final Transfer t : transfers) {
				sess.saveOrUpdate(t);

				i++;
				if (i == 20) {
					sess.flush();
					sess.clear();
					i = 0;
				}
			}

			tx.commit();
		} catch (final RuntimeException e) {
			if (tx != null) {
				tx.rollback();
			}
			e.printStackTrace();

		} finally {
			sess.close();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Query q) {
		return q.list();
	}
}
