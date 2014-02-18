package org.nem.nis.dao;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.nem.nis.model.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BlockDaoImpl implements BlockDao
{
	@Autowired
    private SessionFactory sessionFactory;
    
    private Session getCurrentSession() {
    	return sessionFactory.getCurrentSession();
    }
    
	@Override
	@Transactional
	public void save(Block block) {
		getCurrentSession().saveOrUpdate(block);
	}

	@Override
	@Transactional
	public Long count() {
		return (Long) getCurrentSession().createQuery("select count (*) from Block").uniqueResult();
	}

	@Override
	@Transactional
	public Block findByShortId(long shortId) {
		List<?> userList = new ArrayList<Block>();
        Query query = getCurrentSession()
       		 .createQuery("from Block a where a.shortId = :id")
       		 .setParameter("id", shortId);
        userList = query.list();
        if (userList.size() > 0)
                return (Block)userList.get(0);
        else
                return null;  
	}
}
