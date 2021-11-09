package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.nis.dbmodel.DbAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class AccountDaoImpl implements AccountDao {
	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public AccountDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional(readOnly = true)
	public DbAccount getAccountByPrintableAddress(final String printableAddress) {
		final Query query = this.getCurrentSession().createQuery("from DbAccount a where a.printableKey = :key").setParameter("key",
				printableAddress);
		return firstFromQuery(query);
	}

	private static DbAccount firstFromQuery(final Query query) {
		final List<?> userList = query.list();
		return !userList.isEmpty() ? (DbAccount) userList.get(0) : null;
	}
}
