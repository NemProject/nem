package org.nem.core.dao;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nem.core.dbmodel.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDaoImpl implements AccountDao {
	@Autowired
    private SessionFactory sessionFactory;
    
    private Session getCurrentSession() {
    	return sessionFactory.getCurrentSession();
    }
    
	@Override
	@Transactional
	public Account getAccount(Long id) {
		 List<?> userList = new ArrayList<Account>();
         Query query = getCurrentSession()
        		 .createQuery("from Account a where a.id = :id")
        		 .setParameter("id", id);
         userList = query.list();
         if (userList.size() > 0)
                 return (Account)userList.get(0);
         else
                 return null;    
	}
	
	@Override
	@Transactional
	public Account getAccountByPrintableAddress(byte[] printableAddres) {
		 List<?> userList = new ArrayList<Account>();
         Query query = getCurrentSession()
        		 .createQuery("from Account a where a.printableKey = :key")
        		 .setParameter("key", printableAddres);
         userList = query.list();
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
		return (Long) getCurrentSession().createQuery("select count (*) from Account").uniqueResult();
	}

	@Override
	public void saveMulti(List<Account> recipientsAccounts) {
		Session sess = sessionFactory.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = sess.beginTransaction();
			for (Account a : recipientsAccounts) {
				sess.saveOrUpdate(a);
			}
			tx.commit();
			
		} catch (RuntimeException e) {
			if (tx != null) tx.rollback();
			e.printStackTrace();
			
		} finally {
			sess.close();
		}
	}
	
}
