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
import org.nem.core.model.BlockHeight;
import org.nem.core.model.Hash;
import org.nem.core.model.HashChain;
import org.nem.nis.dbmodel.Block;
import org.nem.core.utils.ByteUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BlockDaoImpl implements BlockDao {

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public BlockDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

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
	public Block findByHeight(final BlockHeight height) {
		Query query = getCurrentSession()
				.createQuery("from Block a where a.height = :height")
				.setParameter("height", height.getRaw());
		return executeSingleQuery(query);
	}

    @Override
    @Transactional
    public HashChain getHashesFrom(final BlockHeight height, int limit) {
		final List<byte[]> blockList = prepareCriteriaGetFor("blockHash", height, limit).list();
        return new HashChain(blockList);
    }

	@Override
	@Transactional
	public List<Long> getDifficultiesFrom(BlockHeight height, int limit) {
		return prepareCriteriaGetFor("difficulty", height, limit).list();
	}

	@Override
	@Transactional
	public List<Integer> getTimestampsFrom(BlockHeight height, int limit) {
		return prepareCriteriaGetFor("timestamp", height, limit).list();
	}

	@Override
	@Transactional
	public void deleteBlocksAfterHeight(final BlockHeight blockHeight) {
		// apparently delete on blocks is not enough, as
		// delete is not cascaded :/
		//
		// "A delete operation only applies to entities of the specified class and its subclasses.
		//  It does not cascade to related entities."

		Query getTxes = getCurrentSession()
				.createQuery("select tx.id from Block b join b.blockTransfers tx where b.height > :height")
				.setParameter("height", blockHeight.getRaw());
		List<Long> txToDelete = getTxes.list();

		Query query = getCurrentSession()
				.createQuery("delete from Block a where a.height > :height")
				.setParameter("height", blockHeight.getRaw());
		query.executeUpdate();

		if (! txToDelete.isEmpty()) {
			Query dropTxes = getCurrentSession()
					.createQuery("delete from Transfer t where t.id in (:ids)")
					.setParameterList("ids", txToDelete);
			dropTxes.executeUpdate();
		}
	}

	private <T> T executeSingleQuery(final Query query) {
		final List<?> blockList = query.list();
		return blockList.size() > 0 ? (T)blockList.get(0) : null;
	}

	private Criteria prepareCriteriaGetFor(String name, BlockHeight height, int limit) {
		return getCurrentSession().createCriteria(Block.class)
				.setMaxResults(limit)
				.add(Restrictions.ge("height", height.getRaw())) // >=
				.setProjection(Projections.property(name));
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional
	public Block findByHash(final Hash blockHash) {
		final byte[] blockHashBytes = blockHash.getRaw();
		long blockId = ByteUtils.bytesToLong(blockHashBytes);
		Query query = getCurrentSession()
				.createQuery("from Block a where a.shortId = :id")
				.setParameter("id", blockId);
		final List<?> blockList = query.list();
		for (int i = 0; i < blockList.size(); ++i) {
			Block block = (Block)blockList.get(i);
			if (Arrays.equals(blockHashBytes, block.getBlockHash().getRaw())) {
				return block;
			}
		}
		return null;
	}
}
