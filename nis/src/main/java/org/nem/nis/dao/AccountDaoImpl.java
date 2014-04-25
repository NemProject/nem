package org.nem.nis.dao;

import java.util.List;

import javax.transaction.Transactional;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nem.nis.dbmodel.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDaoImpl implements AccountDao {

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public AccountDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional
	public Account getAccount(Long id) {
		Query query = getCurrentSession()
				.createQuery("from Account a where a.id = :id")
				.setParameter("id", id);
		List<?> userList = query.list();
		if (userList.size() > 0)
			return (Account)userList.get(0);
		else
			return null;
	}

	@Override
	@Transactional
	public Account getAccountByPrintableAddress(String printableAddress) {
		Query query = getCurrentSession()
				.createQuery("from Account a where a.printableKey = :key")
				.setParameter("key", printableAddress);
		List<?> userList = query.list();
		if (userList.size() > 0)
			return (Account)userList.get(0);
		else
			return null;
	}

	@Override
	@Transactional
	public void save(Account account) {
		getCurrentSession().saveOrUpdate(account);
	}

	@Override
	@Transactional
	public Long count() {
//		return (Long) getCurrentSession()
//				.createCriteria("Account")
//				.setProjection(Projections.rowCount())
//				.uniqueResult();
		return (Long)getCurrentSession().createQuery("select count (*) from Account").uniqueResult();
	}

	@Override
	public void saveMulti(List<Account> recipientsAccounts) {
		Session sess = sessionFactory.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = sess.beginTransaction();
			recipientsAccounts.forEach(sess::saveOrUpdate);
			tx.commit();

		} catch (RuntimeException e) {
			if (tx != null) tx.rollback();
			e.printStackTrace();

		} finally {
			sess.close();
		}
	}

}
