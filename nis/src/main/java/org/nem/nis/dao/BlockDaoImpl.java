package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.dbmodel.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class BlockDaoImpl implements BlockDao {

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public BlockDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional
	public void save(final Block block) {
		this.getCurrentSession().saveOrUpdate(block);
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
	public void updateLastBlockId(final Block block) {
		final Query updateId = this.getCurrentSession().createQuery("UPDATE Block " +
				"set nextBlockId = :nextBlockId " +
				"where id = :blockId");
		updateId.setParameter("nextBlockId", block.getNextBlockId());
		updateId.setParameter("blockId", block.getId());
		updateId.executeUpdate();
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)this.getCurrentSession().createQuery("select count (*) from Block").uniqueResult();
	}

	//region find*
	@Override
	@Transactional(readOnly = true)
	public Block findById(final long id) {
		final Criteria criteria = this.getCurrentSession().createCriteria(Block.class)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.setFetchMode("blockImportanceTransfers", FetchMode.JOIN)
				.add(Restrictions.eq("id", id));
		return this.executeSingleQuery(criteria);
	}

	@Override
	@Transactional(readOnly = true)
	public Block findByHeight(final BlockHeight height) {
		final Criteria criteria = this.getCurrentSession().createCriteria(Block.class)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.setFetchMode("blockImportanceTransfers", FetchMode.JOIN)
				.add(Restrictions.eq("height", height.getRaw()));
		return this.executeSingleQuery(criteria);
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional(readOnly = true)
	public Block findByHash(final Hash blockHash) {
		final byte[] blockHashBytes = blockHash.getRaw();
		final long blockId = ByteUtils.bytesToLong(blockHashBytes);

		final Criteria criteria = this.getCurrentSession().createCriteria(Block.class)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.setFetchMode("blockImportanceTransfers", FetchMode.JOIN)
				.add(Restrictions.eq("shortId", blockId));
		final List<Block> blockList = listAndCast(criteria);

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
	public HashChain getHashesFrom(final BlockHeight height, final int limit) {
		final List<byte[]> blockList = this.prepareCriteriaGetFor("blockHash", height, limit);
		return HashChain.fromRawHashes(blockList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BlockDifficulty> getDifficultiesFrom(final BlockHeight height, final int limit) {
		final List<Long> rawDifficulties = this.prepareCriteriaGetFor("difficulty", height, limit);
		final List<BlockDifficulty> result = new ArrayList<>(rawDifficulties.size());
		for (final Long elem : rawDifficulties) {
			result.add(new BlockDifficulty(elem));
		}
		return result;
		//return rawDifficulties.stream().map(diff -> new BlockDifficulty(diff)).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TimeInstant> getTimeStampsFrom(final BlockHeight height, final int limit) {
		final List<Integer> rawTimeStamps = this.prepareCriteriaGetFor("timeStamp", height, limit);
		return rawTimeStamps.stream().map(obj -> new TimeInstant(obj)).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<Block> getBlocksForAccount(final Account account, final Integer timeStamp, final int limit) {
		final Criteria criteria = this.getCurrentSession().createCriteria(Block.class)
				.setFetchMode("forger", FetchMode.JOIN)
				.setFetchMode("blockTransfers", FetchMode.SELECT)
				.setFetchMode("blockImportanceTransfers", FetchMode.JOIN)
				.add(Restrictions.le("timeStamp", timeStamp))
				.addOrder(Order.desc("timeStamp"))
						// here we were lucky cause blocktransfers is set to select...
				.setMaxResults(limit)
						// nested criteria
				.createCriteria("forger", "f")
				.add(Restrictions.eq("f.printableKey", account.getAddress().getEncoded()));
		return listAndCast(criteria);
	}

	@Override
	@Transactional
	public Collection<Block> getBlocksAfter(final long blockHeight, final int blocksCount) {
		// whatever it takes : DO NOT ADD setMaxResults here!
		final Criteria criteria = this.getCurrentSession().createCriteria(Block.class)
				.setFetchMode("forger", FetchMode.JOIN)
				.setFetchMode("blockTransfers", FetchMode.JOIN)
				.setFetchMode("blockImportanceTransfers", FetchMode.JOIN)
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

		final Query getTxes = this.getCurrentSession()
				.createQuery("select tx.id from Block b join b.blockTransfers tx where b.height > :height")
				.setParameter("height", blockHeight.getRaw());
		final List<Long> txToDelete = listAndCast(getTxes);

		final Query getImportanceTxes = this.getCurrentSession()
				.createQuery("select tx.id from Block b join b.blockImportanceTransfers tx where b.height > :height")
				.setParameter("height", blockHeight.getRaw());
		final List<Long> importanceTxToDelete = listAndCast(getImportanceTxes);

		if (!importanceTxToDelete.isEmpty()) {
			final Query dropTxes = this.getCurrentSession()
					.createQuery("delete from ImportanceTransfer t where t.id in (:ids)")
					.setParameterList("ids", importanceTxToDelete);
			dropTxes.executeUpdate();
		}

		final Query query = this.getCurrentSession()
				.createQuery("delete from Block a where a.height > :height")
				.setParameter("height", blockHeight.getRaw());
		query.executeUpdate();

		if (!txToDelete.isEmpty()) {
			final Query dropTxes = this.getCurrentSession()
					.createQuery("delete from Transfer t where t.id in (:ids)")
					.setParameterList("ids", txToDelete);
			dropTxes.executeUpdate();
		}
	}

	private <T> T executeSingleQuery(final Criteria criteria) {
		final List<T> blockList = listAndCast(criteria);
		return !blockList.isEmpty() ? blockList.get(0) : null;
	}

	private <T> List<T> prepareCriteriaGetFor(final String name, final BlockHeight height, final int limit) {
		final Criteria criteria = this.getCurrentSession().createCriteria(Block.class)
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
