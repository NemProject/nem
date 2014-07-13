package org.nem.nis.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.crypto.Hash;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;
import org.nem.core.utils.ByteUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)getCurrentSession().createQuery("select count (*) from Block").uniqueResult();
	}

	//region find*
	@Override
	@Transactional(readOnly = true)
	public Block findById(long id) {
		final Criteria criteria =  getCurrentSession().createCriteria(Block.class)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.add(Restrictions.eq("id", id));
		return executeSingleQuery(criteria);
	}

	@Override
	@Transactional(readOnly = true)
	public Block findByHeight(final BlockHeight height) {
		final Criteria criteria =  getCurrentSession().createCriteria(Block.class)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.add(Restrictions.eq("height", height.getRaw()));
		return executeSingleQuery(criteria);
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional(readOnly = true)
	public Block findByHash(final Hash blockHash) {
		final byte[] blockHashBytes = blockHash.getRaw();
		long blockId = ByteUtils.bytesToLong(blockHashBytes);

		final Criteria criteria =  getCurrentSession().createCriteria(Block.class)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.add(Restrictions.eq("shortId", blockId));
		final  List<Block> blockList = listAndCast(criteria);

		for (final Object blockObject : blockList) {
			final Block block = (Block)blockObject;
			if (Arrays.equals(blockHashBytes, block.getBlockHash().getRaw())) {
				return block;
			}
		}
		return null;
	}
	//endregion

    @Override
	@Transactional(readOnly = true)
    public HashChain getHashesFrom(final BlockHeight height, int limit) {
		final List<byte[]> blockList = prepareCriteriaGetFor("blockHash", height, limit);
        return HashChain.fromRawHashes(blockList);
    }

	@Override
	@Transactional(readOnly = true)
	public List<BlockDifficulty> getDifficultiesFrom(BlockHeight height, int limit) {
		final List<Long> rawDifficulties = prepareCriteriaGetFor("difficulty", height, limit);
		final List<BlockDifficulty> result = new ArrayList<>(rawDifficulties.size());
		for (final Long elem : rawDifficulties) {
			result.add(new BlockDifficulty(elem));
		}
		return result;
		//return rawDifficulties.stream().map(diff -> new BlockDifficulty(diff)).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TimeInstant> getTimestampsFrom(BlockHeight height, int limit) {
		final List<Integer> rawTimestamps = prepareCriteriaGetFor("timestamp", height, limit);
		return rawTimestamps.stream().map(obj -> new TimeInstant(obj)).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<Block> getBlocksForAccount(final Account account, final Integer timestamp, int limit) {
		final Criteria criteria =  getCurrentSession().createCriteria(Block.class)
				.setFetchMode("forger", FetchMode.JOIN)
				.setFetchMode("blockTransfers", FetchMode.SELECT)
				.add(Restrictions.le("timestamp", timestamp))
				.addOrder(Order.desc("timestamp"))
				// here we were lucky cause blocktransfers is set to select...
				.setMaxResults(limit)
				// nested criteria
				.createCriteria("forger", "f")
				.add(Restrictions.eq("f.printableKey", account.getAddress().getEncoded()));
		return listAndCast(criteria);
	}

	@Override
	@Transactional
	public Collection<Block> getBlocksAfter(long blockHeight, int blocksCount) {
		// whatever it takes : DO NOT ADD setMaxResults here!
        final Criteria criteria =  getCurrentSession().createCriteria(Block.class)
                .setFetchMode("forger", FetchMode.JOIN)
                .setFetchMode("blockTransfers", FetchMode.JOIN)
                .add(Restrictions.gt("height", blockHeight))
                .add(Restrictions.lt("height", blockHeight + (long)blocksCount))
                .addOrder(Order.asc("height"));
        return listAndCast(criteria);
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
		List<Long> txToDelete = listAndCast(getTxes);

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

	private <T> T executeSingleQuery(final Criteria criteria) {
		final List<T> blockList = listAndCast(criteria);
		return blockList.size() > 0 ? blockList.get(0) : null;
	}

	private <T> List<T> prepareCriteriaGetFor(String name, BlockHeight height, int limit) {
		final Criteria criteria =  getCurrentSession().createCriteria(Block.class)
				.setMaxResults(limit)
				.add(Restrictions.ge("height", height.getRaw())) // >=
				.setProjection(Projections.property(name))
				.addOrder(Order.asc("height"));
		return listAndCast(criteria);
	}


	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Query q) {
		return q.list();
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> listAndCast(final Criteria criteria) {
		return criteria.list();
	}
}
