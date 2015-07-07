package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dao.retrievers.MosaicRetriever;
import org.nem.nis.dbmodel.DbMosaic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class MosaicDaoImpl implements ReadOnlyMosaicDao {
	private final SessionFactory sessionFactory;
	private final MosaicRetriever retriever;

	/**
	 * Creates a mosaic dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 */
	@Autowired(required = true)
	public MosaicDaoImpl(final SessionFactory sessionFactory) {
		this(sessionFactory, new MosaicRetriever());
	}

	/**
	 * Creates a mosaic dao implementation.
	 * TODO 20150707 BR -> J: inject retriever as you commented in the namespace dao implementation class?
	 *
	 * @param sessionFactory The session factory.
	 * @param retriever The mosaic retriever.
	 */
	public MosaicDaoImpl(final SessionFactory sessionFactory, final MosaicRetriever retriever) {
		this.sessionFactory = sessionFactory;
		this.retriever = retriever;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbMosaic> getMosaicsForAccount(
			final Account account,
			final NamespaceId namespaceId,
			final Long maxId,
			final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		final Long accountId = DaoUtils.getAccountId(this.getCurrentSession(), account);
		if (null == accountId) {
			return Collections.emptyList();
		}

		return this.retriever.getMosaicsForAccount(
				this.getCurrentSession(),
				accountId,
				namespaceId,
				id,
				limit);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbMosaic> getMosaicsForNamespace(final NamespaceId namespaceId, final Long maxId, final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		return this.retriever.getMosaicsForNamespace(
				this.getCurrentSession(),
				namespaceId,
				id,
				limit);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbMosaic> getMosaics(final Long maxId, final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		return this.retriever.getMosaics(this.getCurrentSession(), id, limit);
	}
}
