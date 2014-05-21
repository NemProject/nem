package org.nem.nis.dao;

import java.util.*;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nem.core.model.Account;
import org.nem.nis.dbmodel.Transfer;
import org.nem.core.utils.ByteUtils;
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
		return sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional
	public void save(Transfer transaction) {
		getCurrentSession().saveOrUpdate(transaction);
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)getCurrentSession().createQuery("select count (*) from Transfer").uniqueResult();
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional(readOnly = true)
	public Transfer findByHash(byte[] txHash) {
		long txId = ByteUtils.bytesToLong(txHash);
		List<?> userList;
		Query query = getCurrentSession()
				.createQuery("from Transfer a where a.shortId = :id")
				.setParameter("id", txId);
		userList = query.list();
		for (final Object transferObject : userList) {
			Transfer transfer = (Transfer)transferObject;
			if (Arrays.equals(txHash, transfer.getTransferHash().getRaw())) {
				return transfer;
			}
		}
		return null;
	}

	// NOTE: this query will also ask for accounts of senders and recipients!
	@Override
	@Transactional(readOnly = true)
	public Collection<Object[]> getTransactionsForAccount(final Account address, final int limit) {
		// TODO: have no idea how to do it using Criteria...
		Query query = getCurrentSession()
				.createQuery("select t, t.block.id from Transfer t where t.recipient.printableKey = :pubkey or t.sender.printableKey = :pubkey")
				.setParameter("pubkey", address.getAddress().getEncoded());
		return listAndCast(query);
	}

	@Override
	public void saveMulti(List<Transfer> transfers) {
		Session sess = sessionFactory.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = sess.beginTransaction();
			int i = 0;
			for (Transfer t : transfers) {
				sess.saveOrUpdate(t);

				i++;
				if (i == 20) {
					sess.flush();
					sess.clear();
					i = 0;
				}
			}
			System.out.println("saving...");
			tx.commit();
			System.out.println("done...");

		} catch (RuntimeException e) {
			if (tx != null) tx.rollback();
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
