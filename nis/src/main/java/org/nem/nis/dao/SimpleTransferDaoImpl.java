package org.nem.nis.dao;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// TODO 20141030 J-G: we might want to consider consolidating our transfer dao tests to test this directly
// and the others can become simpler delegation tests

/**
 * A basic implementation of the SimpleTransferDao.
 * This is mainly an implementation detail of the classes that directly implement the derived classes.
 *
 * @param <TTransfer> The transfer type.
 */
public class SimpleTransferDaoImpl<TTransfer> implements SimpleReadOnlyTransferDao<TTransfer>, SimpleTransferDao<TTransfer> {
	private static final Logger LOGGER = Logger.getLogger(SimpleTransferDao.class.getName());

	private final String tableName;
	private final SessionFactory sessionFactory;
	// TODO 20150110 G-J: do we still need it here?
	private final Function<TTransfer, Hash> transferHashAccessor;

	/**
	 * Creates a transfer dao implementation.
	 *
	 * @param tableName The transfer table name.
	 * @param sessionFactory The session factory.
	 * @param transferHashSupplier A function that can be used to get a transfer hash.
	 */
	public SimpleTransferDaoImpl(
			final String tableName,
			final SessionFactory sessionFactory,
			final Function<TTransfer, Hash> transferHashSupplier) {
		this.tableName = tableName;
		this.sessionFactory = sessionFactory;
		this.transferHashAccessor = transferHashSupplier;
	}

	/**
	 * Gets the current database session.
	 *
	 * @return The current database session.
	 */
	public Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	public Long count() {
		return (Long)this.createQuery("select count (*) from <TABLE_NAME>").uniqueResult();
	}

	@Override
	public TTransfer findByHash(final byte[] txHash) {
		final Query query = this.createQuery("from <TABLE_NAME> a where a.transferHash = :hash")
				.setParameter("hash", txHash);
		return this.getByHashQuery(query);
	}

	@Override
	public TTransfer findByHash(final byte[] txHash, final long maxBlockHeight) {
		final Query query = this.createQuery("from <TABLE_NAME> t where t.transferHash = :hash and t.block.height <= :height")
				.setParameter("hash", txHash)
				.setParameter("height", maxBlockHeight);
		return this.getByHashQuery(query);
	}

	@SuppressWarnings("unchecked")
	private TTransfer getByHashQuery(final Query query) {
		final List<?> userList = query.list();
		return userList.isEmpty() ? null : ((TTransfer)userList.get(0));
	}

	@Override
	public boolean anyHashExists(final Collection<Hash> hashes, final BlockHeight maxBlockHeight) {
		if (hashes.isEmpty()) {
			return false;
		}

		// note: this might look dumb, but doing it this way is 10x faster than usig .setParameterList() on Query
		final String rawQuery = String.format(
				"Select count(*) from <TABLE_NAME> t where t.transferHash in (%s) and t.block.height <= :height",
				StringUtils.join(hashes.stream().map(h -> String.format("'%s'", h)).collect(Collectors.toList()), ","));
		final Query query = this.createQuery(rawQuery)
				.setParameter("height", maxBlockHeight.getRaw());

		final Long count = (Long)query.uniqueResult();
		return 0 != count;
	}

	@Override
	public void save(final TTransfer entity) {
		this.getCurrentSession().saveOrUpdate(entity);
	}

	@Override
	public void saveMulti(final List<TTransfer> transfers) {
		final Session sess = this.sessionFactory.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = sess.beginTransaction();
			int i = 0;
			for (final TTransfer t : transfers) {
				sess.saveOrUpdate(t);

				i++;
				if (i == 20) {
					sess.flush();
					sess.clear();
					i = 0;
				}
			}

			tx.commit();
		} catch (final RuntimeException e) {
			if (tx != null) {
				tx.rollback();
			}

			LOGGER.severe(String.format("saveMulti failed: %s", e));
		} finally {
			sess.close();
		}
	}

	private Query createQuery(final String rawQuery) {
		return this.getCurrentSession()
				.createQuery(rawQuery.replace("<TABLE_NAME>", this.tableName));
	}
}