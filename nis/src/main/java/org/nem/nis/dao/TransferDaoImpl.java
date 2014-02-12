package org.nem.nis.dao;

import java.util.List;

import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nem.nis.model.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


@Repository
public class TransferDaoImpl implements TransferDao
{    
	@Autowired
	private SessionFactory sessionFactory;
    
    private Session getCurrentSession() {
    	return sessionFactory.getCurrentSession();
    }
    
    @Override
	@Transactional
	public void save(Transfer transaction) {
		getCurrentSession().saveOrUpdate(transaction);
	}

	@Override
	@Transactional
	public Long count() {
		return (Long) getCurrentSession().createQuery("select count (*) from Transfer").uniqueResult();
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
}
