package org.nem.nis.dao;

import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.nem.nis.dbmodel.Block;
import org.nem.core.utils.ByteUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BlockDaoImpl implements BlockDao {
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

	/*
	 * I was trying to do that, by marking Block with:
	 * "@DynamicUpdate" and "@SelectBeforeUpdate", and indeed,
	 * only nextBlockId was updated, but apart from that also all associated
	 * transfers and block_transfers table. That is unacceptable of course.
	 *
	 * If anyone will figure how to do that we could get rid of this.
	 */
	@Override
	@Transactional
	public void updateLastBlockId(Block block) {
		Query updateId = getCurrentSession().createQuery("UPDATE Block " +
				"set nextBlockId = :nextBlockId " +
				"where id = :blockId");
		updateId.setParameter("nextBlockId", block.getNextBlockId());
		updateId.setParameter("blockId", block.getId());
		updateId.executeUpdate();
	}

	@Override
	@Transactional
	public Long count() {
		return (Long)getCurrentSession().createQuery("select count (*) from Block").uniqueResult();
	}

	@Override
	@Transactional
	public Block findById(long id) {
		Query query = getCurrentSession()
				.createQuery("from Block a where a.id = :id")
				.setParameter("id", id);
		return executeSingleQuery(query);
	}

	@Override
	@Transactional
	public Block findByHeight(long blockHeight) {
		Query query = getCurrentSession()
				.createQuery("from Block a where a.height = :height")
				.setParameter("height", blockHeight);
		return executeSingleQuery(query);
	}

    @Override
    @Transactional
    public List<byte[]> getHashesFrom(long blockHeight, int limit) {
        Criteria criteria = getCurrentSession().createCriteria(Block.class)
                .setMaxResults(limit)
                .add(Restrictions.ge("height", blockHeight)) // >=
                .setProjection(Projections.property("blockHash"));
        final List<byte[]> blockList = criteria.list();
        return blockList;
    }

    private <T> T executeSingleQuery(final Query query) {
		final List<?> blockList = query.list();
		return blockList.size() > 0 ? (T)blockList.get(0) : null;
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional
	public Block findByHash(byte[] blockHash) {
		long blockId = ByteUtils.bytesToLong(blockHash);
		Query query = getCurrentSession()
				.createQuery("from Block a where a.shortId = :id")
				.setParameter("id", blockId);
		final List<?> blockList = query.list();
		for (int i = 0; i < blockList.size(); ++i) {
			Block block = (Block)blockList.get(i);
			if (Arrays.equals(blockHash, block.getBlockHash())) {
				return block;
			}
		}
		return null;
	}
}
