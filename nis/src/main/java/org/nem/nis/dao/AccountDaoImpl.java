package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.nis.dbmodel.Account;
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
	public Account getAccount(final Long id) {
		final Query query = this.getCurrentSession()
				.createQuery("from Account a where a.id = :id")
				.setParameter("id", id);
		return firstFromQuery(query);
	}

	@Override
	@Transactional(readOnly = true)
	public Account getAccountByPrintableAddress(final String printableAddress) {
		final Query query = this.getCurrentSession()
				.createQuery("from Account a where a.printableKey = :key")
				.setParameter("key", printableAddress);
		return firstFromQuery(query);
	}

	@Override
	@Transactional
	public void save(final Account account) {
		this.getCurrentSession().saveOrUpdate(account);
	}

	private static Account firstFromQuery(final Query query) {
		final List<?> userList = query.list();
		return !userList.isEmpty() ? (Account)userList.get(0) : null;
	}
}
