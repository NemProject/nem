package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;
import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.dbmodel.DbBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
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
	public void save(final DbBlock block) {
		this.getCurrentSession().saveOrUpdate(block);
	}

	// TODO 20141206 J-G: does it make sense to add a test for this?
	@Override
	@Transactional
	public void save(final List<DbBlock> dbBlocks) {
		for (final DbBlock block : dbBlocks) {
			this.getCurrentSession().saveOrUpdate(block);
		}
		this.getCurrentSession().flush();
		this.getCurrentSession().clear();
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)this.getCurrentSession().createQuery("select count (*) from DbBlock").uniqueResult();
	}

	// NOTE: remember to modify deleteBlocksAfterHeight TOO!
	private static Criteria setTransfersFetchMode(final Criteria criteria, final FetchMode fetchMode) {
		return criteria
				.setFetchMode("blockTransferTransactions", fetchMode)
				.setFetchMode("blockImportanceTransferTransactions", fetchMode)
				.setFetchMode("blockMultisigAggregateModificationTransactions", fetchMode)
				.setFetchMode("blockMultisigTransactions", fetchMode);
	}

	private static Criteria setTransfersToJoin(final Criteria criteria) {
		return setTransfersFetchMode(criteria, FetchMode.JOIN);
	}

	private static Criteria setTransfersToSelect(final Criteria criteria) {
		return setTransfersFetchMode(criteria, FetchMode.SELECT);
	}

	//region find*
	@Override
	@Transactional(readOnly = true)
	public DbBlock findByHeight(final BlockHeight height) {
		final Criteria criteria = setTransfersToJoin(this.getCurrentSession().createCriteria(DbBlock.class))
				.add(Restrictions.eq("height", height.getRaw()));
		return this.executeSingleQuery(criteria);
	}

	/**
	 * First try to find block using "shortId",
	 * than find proper block in software.
	 */
	@Override
	@Transactional(readOnly = true)
	public DbBlock findByHash(final Hash blockHash) {
		final byte[] blockHashBytes = blockHash.getRaw();
		final long blockId = ByteUtils.bytesToLong(blockHashBytes);

		final Criteria criteria = setTransfersToJoin(this.getCurrentSession().createCriteria(DbBlock.class))
				.add(Restrictions.eq("shortId", blockId));
		final List<DbBlock> blockList = listAndCast(criteria);

		for (final Object blockObject : blockList) {
			final DbBlock block = (DbBlock)blockObject;
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
		return rawTimeStamps.stream().map(TimeInstant::new).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbBlock> getBlocksForAccount(final Account account, final Hash hash, final int limit) {
		final long height = null == hash ? Long.MAX_VALUE : this.findByHash(hash).getHeight();
		return this.getLatestBlocksForAccount(account, height, limit);
	}

	private Collection<DbBlock> getLatestBlocksForAccount(final Account account, final long height, final int limit) {
		// NOTE: there was JOIN used for importanceTransfers here, that was a bug
		final Criteria criteria = setTransfersToSelect(this.getCurrentSession().createCriteria(DbBlock.class))
				.setFetchMode("harvester", FetchMode.JOIN)
				.setFetchMode("lessor", FetchMode.JOIN)
				.add(Restrictions.lt("height", height))
				.addOrder(Order.desc("height"))
						// setMaxResults limits results, not objects (so in case of join it could be block with
						// many TXes), but this will work correctly cause blockTransferTransactions is set to select...
				.setMaxResults(limit)
						// nested criteria
				.createAlias("harvester", "f")
				.createAlias("lessor", "l", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.disjunction(
						Restrictions.eq("f.printableKey", account.getAddress().getEncoded()),
						Restrictions.eq("l.printableKey", account.getAddress().getEncoded())
				));
		return listAndCast(criteria);
	}

	@Override
	@Transactional
	public Collection<DbBlock> getBlocksAfter(final BlockHeight height, final int limit) {
		// whatever it takes : DO NOT ADD setMaxResults here!
		final long blockHeight = height.getRaw();
		final Criteria criteria = setTransfersToJoin(this.getCurrentSession().createCriteria(DbBlock.class))
				.setFetchMode("harvester", FetchMode.JOIN)
				.add(Restrictions.gt("height", blockHeight))
				.add(Restrictions.le("height", blockHeight + limit))
				.addOrder(Order.asc("height"));
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
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

		this.dropTransfers(blockHeight, "DbTransferTransaction", "blockTransferTransactions", v -> {});
		this.dropTransfers(blockHeight, "DbImportanceTransferTransaction", "blockImportanceTransferTransactions", v -> {});
		this.dropTransfers(
				blockHeight,
				"DbMultisigAggregateModificationTransaction",
				"blockMultisigAggregateModificationTransactions",
				transactionsToDelete -> {
					final Query preQuery = this.getCurrentSession()
							.createQuery("delete from DbMultisigModification m where m.multisigAggregateModificationTransaction.id in (:ids)")
							.setParameterList("ids", transactionsToDelete);
					preQuery.executeUpdate();
				});

		// must be last
		this.dropTransfers(
				blockHeight,
				"DbMultisigTransaction",
				"blockMultisigTransactions",
				transactionsToDelete -> {
					final Query preQuery = this.getCurrentSession()
							.createQuery("delete from DbMultisigSignatureTransaction m where m.multisigTransaction.id in (:ids)")
							.setParameterList("ids", transactionsToDelete);
					preQuery.executeUpdate();
				});

		final Query query = this.getCurrentSession()
				.createQuery("delete from DbBlock a where a.height > :height")
				.setParameter("height", blockHeight.getRaw());
		query.executeUpdate();
	}

	private void dropTransfers(final BlockHeight blockHeight, final String tableName, final String transfersName, final Consumer<List<Long>> preQuery) {
		final Query getTransactionIdsQuery = this.getCurrentSession()
				.createQuery("select tx.id from DbBlock b join b." + transfersName + " tx where b.height > :height")
				.setParameter("height", blockHeight.getRaw());
		final List<Long> transactionsToDelete = listAndCast(getTransactionIdsQuery);

		if (!transactionsToDelete.isEmpty()) {
			preQuery.accept(transactionsToDelete);

			final Query dropTxes = this.getCurrentSession()
					.createQuery("delete from " + tableName + " t where t.id in (:ids)")
					.setParameterList("ids", transactionsToDelete);
			dropTxes.executeUpdate();
		}
	}

	private <T> T executeSingleQuery(final Criteria criteria) {
		final List<T> blockList = listAndCast(criteria);
		return !blockList.isEmpty() ? blockList.get(0) : null;
	}

	private <T> List<T> prepareCriteriaGetFor(final String name, final BlockHeight height, final int limit) {
		final Criteria criteria = this.getCurrentSession().createCriteria(DbBlock.class)
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
